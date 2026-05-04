package com.example.camerarental.order.integration;

import com.example.camerarental.common.enums.DeviceStatus;
import com.example.camerarental.common.mapper.OperationLogMapper;
import com.example.camerarental.common.result.Result;
import com.example.camerarental.common.exception.GlobalExceptionHandler;
import com.example.camerarental.order.controller.OrderController;
import com.example.camerarental.order.dto.CouponDTO;
import com.example.camerarental.order.dto.DeviceDTO;
import com.example.camerarental.order.entity.Order;
import com.example.camerarental.order.feign.CouponFeignClient;
import com.example.camerarental.order.feign.DeviceFeignClient;
import com.example.camerarental.order.mapper.OrderMapper;
import com.example.camerarental.order.mapper.EvaluationMapper;
import com.example.camerarental.order.mapper.MqOutboxMessageMapper;
import com.example.camerarental.order.mq.OrderNotificationProducer;
import com.example.camerarental.order.service.OperationLogQueryService;
import com.example.camerarental.order.service.OrderService;
import com.example.camerarental.order.service.OutboxMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("Layer1")
@WebMvcTest(controllers = OrderController.class)
@Import({OrderService.class, GlobalExceptionHandler.class})
class OrderCreateL1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderMapper orderMapper;

    @MockBean
    private EvaluationMapper evaluationMapper;

    @MockBean
    private MqOutboxMessageMapper mqOutboxMessageMapper;

    @MockBean
    private OperationLogMapper operationLogMapper;

    @MockBean
    private DeviceFeignClient deviceFeignClient;

    @MockBean
    private CouponFeignClient couponFeignClient;

    @MockBean
    private OrderNotificationProducer orderNotificationProducer;

    @MockBean
    private OutboxMetricsService outboxMetricsService;

    @MockBean
    private OperationLogQueryService operationLogQueryService;

    @BeforeEach
    void setUp() {
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(deviceFeignClient.getById(anyLong())).thenReturn(Result.success(activeDevice(2002L)));
    }

    @Test
    void shouldRejectWhenRentalPeriodInvalid() throws Exception {
        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1001")
                        .content(createPayload(101L, "2026-05-10T10:00:00", "2026-05-09T10:00:00", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("租赁开始时间不能晚于结束时间"));
    }

    @Test
    void shouldRejectWhenRentingOwnDevice() throws Exception {
        when(deviceFeignClient.getById(101L)).thenReturn(Result.success(activeDevice(1001L)));

        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1001")
                        .content(createPayload(101L, "2026-05-01T10:00:00", "2026-05-03T10:00:00", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("不能租赁自己发布的设备"));
    }

    @Test
    void shouldReturnSystemErrorWhenDeviceServiceFallbackTriggered() throws Exception {
        // 模拟 Sentinel/断路后 DeviceFeign fallback 返回 code != 0
        when(deviceFeignClient.getById(101L)).thenReturn(Result.error("设备服务暂时不可用，请稍后重试"));

        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1001")
                        .content(createPayload(101L, "2026-05-01T10:00:00", "2026-05-03T10:00:00", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("设备服务不可用，暂时无法创建订单"));
    }

    @Test
    void shouldReturnSystemErrorWhenCouponServiceFallbackTriggered() throws Exception {
        // 模拟 Sentinel/断路后 CouponFeign fallback 返回 code != 0
        when(couponFeignClient.getById(9001L)).thenReturn(Result.error("优惠券服务暂时不可用，请稍后重试"));

        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1001")
                        .content(createPayload(101L, "2026-05-01T10:00:00", "2026-05-03T10:00:00", 9001L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("优惠券服务不可用，暂时无法校验优惠券"));
    }

    @Test
    void shouldRejectWhenCouponOffline() throws Exception {
        CouponDTO offlineCoupon = CouponDTO.builder()
                .id(9001L)
                .status("OFFLINE")
                .denomination(new BigDecimal("30"))
                .threshold(new BigDecimal("100"))
                .build();
        when(couponFeignClient.getById(9001L)).thenReturn(Result.success(offlineCoupon));

        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1001")
                        .content(createPayload(101L, "2026-05-01T10:00:00", "2026-05-03T10:00:00", 9001L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5004))
                .andExpect(jsonPath("$.message").value("优惠券不可用或已下架"));
    }

    @Test
    void shouldRejectWhenCouponThresholdNotMet() throws Exception {
        CouponDTO thresholdCoupon = CouponDTO.builder()
                .id(9002L)
                .status("PUBLISHED")
                .denomination(new BigDecimal("20"))
                .threshold(new BigDecimal("500"))
                .build();
        when(couponFeignClient.getById(9002L)).thenReturn(Result.success(thresholdCoupon));

        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1001")
                        .content(createPayload(101L, "2026-05-01T10:00:00", "2026-05-03T10:00:00", 9002L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5005))
                .andExpect(jsonPath("$.message").value("未满足优惠券使用门槛"));
    }

    private static DeviceDTO activeDevice(Long ownerId) {
        return DeviceDTO.builder()
                .id(101L)
                .userId(ownerId)
                .rentPrice(new BigDecimal("100"))
                .deposit(new BigDecimal("500"))
                .status(DeviceStatus.ACTIVE.getValue())
                .build();
    }

    private static String createPayload(Long deviceId, String startTime, String endTime, Long couponId) {
        String coupon = couponId == null ? "null" : couponId.toString();
        return String.format(
                "{\"deviceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\",\"couponId\":%s}",
                deviceId,
                startTime,
                endTime,
                coupon
        );
    }
}

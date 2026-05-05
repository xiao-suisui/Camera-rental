package com.example.camerarental.device.mq;

import com.example.camerarental.common.mq.MqConstants;
import com.example.camerarental.common.mq.OrderCancelledEvent;
import com.example.camerarental.device.entity.MqConsumeLog;
import com.example.camerarental.device.mapper.MqConsumeLogMapper;
import com.example.camerarental.device.service.DeviceService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledReleaseStockConsumer {

    @Value("${mq.consume-log.consumer-name:${spring.application.name}:order-cancelled-release-stock-consumer}")
    private String consumerName;

    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final DeviceService deviceService;

    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = MqConstants.DEVICE_SYNC_QUEUE)
    public void consume(OrderCancelledEvent event,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String eventId = event.getEventId();
        try {
            if (eventId == null || eventId.isBlank()) {
                log.error("Discard order-cancelled event without eventId: {}", event);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            try {
                mqConsumeLogMapper.insert(MqConsumeLog.builder()
                        .eventId(eventId)
                        .consumer(consumerName)
                        .consumedAt(LocalDateTime.now())
                        .build());
            } catch (DuplicateKeyException duplicateKeyException) {
                log.info("Skip duplicate order-cancelled stock event: eventId={}", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            deviceService.handleOrderCancelledReleaseStock(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to consume order-cancelled stock event: eventId={}", eventId, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}


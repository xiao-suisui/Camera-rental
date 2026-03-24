package com.example.camerarental.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long deviceId;
    private Long lessorId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalRent;
    private BigDecimal deposit;
    private Long couponId;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String virtualOrderNo;
    private String status;
    private String depositStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


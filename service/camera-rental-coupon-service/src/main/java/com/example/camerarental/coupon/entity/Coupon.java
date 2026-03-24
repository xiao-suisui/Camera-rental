package com.example.camerarental.coupon.entity;

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
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private BigDecimal denomination;
    private BigDecimal threshold;
    private Integer totalCount;
    private Integer usedCount;
    private Integer validDays;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


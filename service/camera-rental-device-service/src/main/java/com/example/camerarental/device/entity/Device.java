package com.example.camerarental.device.entity;

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
@TableName("device")
public class Device {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String brand;
    private String model;
    private String description;
    private String imagePaths;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private String address;
    private String auditStatus;
    private String status;
    private String batchId;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


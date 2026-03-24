package com.example.camerarental.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device_favorite")
public class DeviceFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deviceId;

    private LocalDateTime createdAt;
}


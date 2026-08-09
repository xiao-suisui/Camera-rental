package com.example.camerarental.common.entity;

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
@TableName("operation_log")
public class OperationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;
    private String description;
    private String method;
    private String url;
    private String requestMethod;
    private String requestParams;
    private String responseResult;
    private Long userId;
    private String ip;
    private Long executionTime;
    private String status;
    private String errorMsg;
    private LocalDateTime createdAt;
}

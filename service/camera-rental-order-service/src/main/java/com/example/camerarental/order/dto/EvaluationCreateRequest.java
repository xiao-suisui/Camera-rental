package com.example.camerarental.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationCreateRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "评价人ID不能为空")
    private Long evaluatorId;

    @NotNull(message = "被评价人ID不能为空")
    private Long evaluatedId;

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer score;

    @NotNull(message = "评价内容不能为空")
    private String content;

    private String imagePaths; // 可选，图片路径（逗号分隔）
}


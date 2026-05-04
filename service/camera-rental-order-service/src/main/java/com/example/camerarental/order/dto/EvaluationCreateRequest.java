package com.example.camerarental.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EvaluationCreateRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private Long evaluatorId;

    private Long evaluatedId;

    private Long deviceId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer score;

    @NotBlank(message = "评价内容不能为空")
    @Size(min = 1, max = 1000, message = "评价内容长度需在1-1000字之间")
    private String content;

    private String imagePaths; // 可选，图片路径（逗号分隔）
}


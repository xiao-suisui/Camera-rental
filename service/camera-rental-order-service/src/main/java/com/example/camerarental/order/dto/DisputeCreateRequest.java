package com.example.camerarental.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisputeCreateRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "纠纷类型不能为空")
    private String disputeType;

    @NotBlank(message = "纠纷描述不能为空")
    @Size(min = 10, max = 2000, message = "纠纷描述需10-2000字")
    private String description;
}

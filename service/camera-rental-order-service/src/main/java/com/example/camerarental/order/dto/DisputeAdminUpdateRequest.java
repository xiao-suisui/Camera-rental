package com.example.camerarental.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisputeAdminUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    @Size(max = 2000)
    private String adminRemark;
}

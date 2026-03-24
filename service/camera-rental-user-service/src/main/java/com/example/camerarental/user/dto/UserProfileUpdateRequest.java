package com.example.camerarental.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String address;
}


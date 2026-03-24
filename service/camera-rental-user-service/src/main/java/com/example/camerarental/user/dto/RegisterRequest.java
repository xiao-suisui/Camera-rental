package com.example.camerarental.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 64, message = "password length must be 6-64")
    private String password;

    @NotBlank(message = "confirmPassword is required")
    private String confirmPassword;

    @NotBlank(message = "nickname is required")
    private String nickname;
}


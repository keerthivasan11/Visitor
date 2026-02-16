package com.smartsecurity.system.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class NotificationRequest {
    @NotNull
    private Integer userId;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

}

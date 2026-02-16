package com.smartsecurity.system.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class SaveFcmTokenRequest {

    @NotBlank
    private String fcmToken;
}

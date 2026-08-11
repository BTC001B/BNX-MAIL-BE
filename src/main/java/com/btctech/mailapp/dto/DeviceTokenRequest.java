package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceTokenRequest {
    
    @NotBlank(message = "Device token is required")
    private String deviceToken;
    
    @NotBlank(message = "Device type is required")
    private String deviceType;
}

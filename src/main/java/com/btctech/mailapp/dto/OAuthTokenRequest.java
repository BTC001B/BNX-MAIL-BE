package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthTokenRequest {
    @NotBlank(message = "grant_type is required")
    private String grantType; // Should be "authorization_code"

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "client_id is required")
    private String clientId;

    @NotBlank(message = "client_secret is required")
    private String clientSecret;
}

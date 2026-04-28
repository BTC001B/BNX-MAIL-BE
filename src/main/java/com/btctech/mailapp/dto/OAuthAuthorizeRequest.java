package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthAuthorizeRequest {
    @NotBlank(message = "client_id is required")
    private String clientId;

    @NotBlank(message = "redirect_uri is required")
    private String redirectUri;

    private String state;
}

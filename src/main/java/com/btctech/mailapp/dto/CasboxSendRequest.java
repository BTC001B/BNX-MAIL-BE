package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CasboxSendRequest {
    @NotBlank(message = "Receiver email is required")
    private String receiverEmail;
    
    private String subject;
    
    @NotBlank(message = "Body is required")
    private String body;
}

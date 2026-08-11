package com.btctech.mailapp.dto;

import lombok.Data;

@Data
public class IncomingMailWebhookRequest {
    private String toEmail;
    private String fromEmail;
    private String subject;
    // We can add more fields if needed, like plain text snippet
}

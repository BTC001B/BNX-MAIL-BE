package com.btctech.mailapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SendMailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid recipient email format")
    private String to;

    @Email(message = "Invalid CC email format")
    private String cc;

    @Email(message = "Invalid BCC email format")
    private String bcc;

    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject cannot exceed 500 characters")
    private String subject;

    @NotBlank(message = "Email body is required")
    private String body;

    private Boolean isHtml = false;
}

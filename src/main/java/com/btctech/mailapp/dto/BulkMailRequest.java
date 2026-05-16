package com.btctech.mailapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class BulkMailRequest {

    @NotEmpty(message = "Recipient list cannot be empty")
    private List<String> recipients;

    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject cannot exceed 500 characters")
    private String subject;

    @NotBlank(message = "Email body is required")
    private String body;

    private Boolean isHtml = true;
    
    private List<AttachmentInfo> attachments;
}

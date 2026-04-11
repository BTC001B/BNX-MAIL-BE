package com.btctech.mailapp.dto;

import lombok.Data;

@Data
public class DraftRequest {
    private Long id; // Null for new drafts
    private Long mailAccountId;
    private Long userId; // The person currently editing
    private String to;
    private String cc;
    private String bcc;
    private String subject;
    private String body;
    private Boolean isHtml = false;
}

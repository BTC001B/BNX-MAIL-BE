package com.btctech.mailapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailResponse {
    private String uid;
    private String messageId;
    private String from;
    private String to;
    private String subject;
    private String bodyPreview;
    private String sentDate;
    private String receivedDate;
    private boolean isRead;
    private boolean hasAttachments;
    private long sizeBytes;
}

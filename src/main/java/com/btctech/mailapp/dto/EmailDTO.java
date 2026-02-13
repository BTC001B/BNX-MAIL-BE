package com.btctech.mailapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDTO {
    private String uid;
    private String messageId;
    private String from;
    private String to;
    private String subject;
    private String body;
    private String htmlBody;
    private Date sentDate;
    private Date receivedDate;
    private boolean isRead;
    private boolean hasAttachments;
    private List<String> attachments;
    private int size;
}
package com.btctech.mailapp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CasboxMessageDto {
    private Long id;
    private String senderEmail;
    private String receiverEmail;
    private String subject;
    private String body;
    private String attachmentsJson;
    private String status;
    private LocalDateTime timestamp;
}

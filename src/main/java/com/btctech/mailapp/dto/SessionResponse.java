package com.btctech.mailapp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private String ipAddress;
    private String userAgent;
    private String location;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean isCurrentSession;
}

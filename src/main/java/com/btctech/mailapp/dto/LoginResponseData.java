package com.btctech.mailapp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LoginResponseData {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String accountType;
    private Boolean isPrimary;

    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn;
    private Long refreshTokenExpiresIn;
    
    private String profilePicture;
    private String profilePictureUrl;

    private List<MailboxSummary> mailboxes;

    private Boolean isAutoUpgraded;
    private LocalDateTime loginAt;

    @Data
    @Builder
    public static class MailboxSummary {
        private Long emailId;
        private String email;
        private Boolean isPrimary;
    }
}

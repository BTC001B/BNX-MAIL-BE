package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String toRecipient;

    @Column(name = "cc_recipient")
    private String cc;

    @Column(name = "bcc_recipient")
    private String bcc;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    private String fromName;

    @Column(name = "is_html")
    @Builder.Default
    private Boolean isHtml = false;

    @Column(columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Builder.Default
    private boolean processed = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

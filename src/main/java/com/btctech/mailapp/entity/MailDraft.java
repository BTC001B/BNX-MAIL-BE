package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mail_drafts")
public class MailDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mail_account_id", nullable = false)
    private Long mailAccountId;

    @Column(name = "to_address", columnDefinition = "TEXT")
    private String to;

    @Column(name = "cc_address", columnDefinition = "TEXT")
    private String cc;

    @Column(name = "bcc_address", columnDefinition = "TEXT")
    private String bcc;

    private String subject;

    @Column(columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "is_html")
    private Boolean isHtml = false;

    @Column(name = "last_opened_at")
    private java.time.LocalDateTime lastOpenedAt;

    @Column(name = "attachments_json", columnDefinition = "LONGTEXT")
    private String attachmentsJson;

    @Column(name = "status")
    private String status = "DRAFT";

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

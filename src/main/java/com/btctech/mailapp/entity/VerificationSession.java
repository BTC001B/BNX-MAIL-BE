package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "verification_sessions")
public class VerificationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "mail_account_id", nullable = false)
    private Long mailAccountId;

    @Column(name = "reference_id", unique = true, nullable = false)
    private String referenceId;

    @Column(name = "verification_id")
    private String verificationId;

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, AUTHENTICATED, FAILED, EXPIRED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

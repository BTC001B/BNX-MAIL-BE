package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "abuse_appeals")
public class Appeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_user_id", nullable = false)
    private User bannedUser;

    @Column(name = "appeal_message", nullable = false, length = 2000)
    private String appealMessage;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AppealStatus status = AppealStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AppealStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}

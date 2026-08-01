package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "abuse_reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter; // The user who submitted the report

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser; // The spammer/offender

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "reported_email_subject", length = 500)
    private String reportedEmailSubject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

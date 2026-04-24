package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "snoozed_emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnoozedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private String originalFolderName;

    @Column(nullable = false)
    private LocalDateTime wakeUpAt;

    @Builder.Default
    private boolean processed = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}


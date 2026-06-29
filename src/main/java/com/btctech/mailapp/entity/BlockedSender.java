package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_senders", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userEmail", "blockedEmail"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedSender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String blockedEmail;

    @Builder.Default
    @Column(name = "blocked_at", nullable = false, updatable = false)
    private LocalDateTime blockedAt = LocalDateTime.now();
}

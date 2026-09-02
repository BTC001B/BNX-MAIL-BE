package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_contacts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_email", "blocked_email"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "blocked_email", nullable = false)
    private String blockedEmail;

    @Builder.Default
    @Column(name = "blocked_at", nullable = false, updatable = false)
    private LocalDateTime blockedAt = LocalDateTime.now();
}

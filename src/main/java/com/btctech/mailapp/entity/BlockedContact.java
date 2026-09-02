package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_contacts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_blocked_email", columnNames = {"user_email", "blocked_email"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "blocked_email", nullable = false, length = 150)
    private String blockedEmail;

    @Column(name = "blocked_at", nullable = false, updatable = false)
    private LocalDateTime blockedAt;

    @PrePersist
    protected void onCreate() {
        if (this.blockedAt == null) {
            this.blockedAt = LocalDateTime.now();
        }
    }
}

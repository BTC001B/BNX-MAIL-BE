package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mail_accounts")
public class MailAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "domain_id", nullable = false)
    private Long domainId;

    @Column(name = "email_name", nullable = false, length = 50)
    private String emailName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "maildir_path", nullable = false, length = 500)
    private String maildirPath;

    @Column(name = "password")
    private String password;

    @Column(name = "quota")
    private Long quota;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
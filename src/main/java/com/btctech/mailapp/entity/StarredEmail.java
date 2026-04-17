package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "starred_emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarredEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private String folderName;

    @Builder.Default
    private LocalDateTime starredAt = LocalDateTime.now();
}

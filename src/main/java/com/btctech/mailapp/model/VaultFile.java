package com.btctech.mailapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "vault_files")
@Data
@NoArgsConstructor
public class VaultFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public VaultFile(String filename, String contentType, Long size, byte[] data, String ownerEmail) {
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.data = data;
        this.ownerEmail = ownerEmail;
        this.uploadedAt = LocalDateTime.now();
    }
}

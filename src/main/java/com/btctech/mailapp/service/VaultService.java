package com.btctech.mailapp.service;

import com.btctech.mailapp.model.VaultFile;
import com.btctech.mailapp.repository.VaultFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class VaultService {

    @Autowired
    private VaultFileRepository vaultFileRepository;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public VaultFile uploadFile(MultipartFile file, String ownerEmail) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the 5MB limit");
        }

        VaultFile vaultFile = new VaultFile(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes(),
                ownerEmail
        );

        return vaultFileRepository.save(vaultFile);
    }

    public List<VaultFileRepository.VaultFileMetadata> getUserFiles(String ownerEmail) {
        return vaultFileRepository.findByOwnerEmailOrderByUploadedAtDesc(ownerEmail);
    }

    public VaultFile getFile(Long id, String ownerEmail) {
        VaultFile file = vaultFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
        
        if (!file.getOwnerEmail().equals(ownerEmail)) {
            throw new RuntimeException("Unauthorized access to file");
        }

        return file;
    }

    public void deleteFile(Long id, String ownerEmail) {
        VaultFile file = getFile(id, ownerEmail);
        vaultFileRepository.delete(file);
    }
}

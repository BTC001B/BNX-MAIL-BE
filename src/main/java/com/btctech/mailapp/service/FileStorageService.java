package com.btctech.mailapp.service;

import com.btctech.mailapp.exception.MailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads/drafts}")
    private String uploadDir;

    /**
     * Save file to disk
     */
    public String saveFile(Long draftId, MultipartFile file) {
        try {
            Path draftPath = Paths.get(uploadDir).resolve(draftId.toString());
            if (!Files.exists(draftPath)) {
                Files.createDirectories(draftPath);
            }

            String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            // Collision prevention: append timestamp
            String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
            Path targetLocation = draftPath.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return targetLocation.toString();
        } catch (IOException ex) {
            log.error("Could not store file for draft {}. Error: {}", draftId, ex.getMessage());
            throw new MailException("Could not store file. Please try again!");
        }
    }

    /**
     * Delete a specific file
     */
    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ex) {
            log.error("Could not delete file: {}. Error: {}", filePath, ex.getMessage());
        }
    }

    /**
     * Delete entire draft directory
     */
    public void deleteDraftDirectory(Long draftId) {
        try {
            Path draftPath = Paths.get(uploadDir).resolve(draftId.toString());
            if (Files.exists(draftPath)) {
                Files.walk(draftPath)
                    .sorted((p1, p2) -> p2.compareTo(p1)) // Delete files before directory
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("Failed to delete path: {}", path);
                        }
                    });
            }
        } catch (IOException ex) {
            log.error("Could not delete draft directory for {}. Error: {}", draftId, ex.getMessage());
        }
    }
}

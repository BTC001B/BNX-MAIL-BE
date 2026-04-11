package com.btctech.mailapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving draft media (thumbnails).
 * Enforces security by resolving paths relative to the draft directory.
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private static final String UPLOAD_DIR = "uploads/drafts";

    /**
     * Stream thumbnail preview for a draft attachment.
     * URI: /api/media/preview/{draftId}/{thumbFileName}
     */
    @GetMapping("/preview/{draftId}/{thumbFileName}")
    public ResponseEntity<Resource> getThumbnail(
            @PathVariable Long draftId,
            @PathVariable String thumbFileName) {
        
        try {
            // Path: uploads/drafts/{draftId}/thumbnails/{thumbFileName}
            Path filePath = Paths.get(UPLOAD_DIR, String.valueOf(draftId), "thumbnails", thumbFileName);
            File file = filePath.toFile();

            if (!file.exists()) {
                log.warn("Thumbnail not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            
            // Determine content type
            String contentType = "image/jpeg"; // Thumbnails are JPG by default in ImagePreviewService
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + thumbFileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error streaming thumbnail: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

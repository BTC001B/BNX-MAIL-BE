package com.btctech.mailapp.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for generating image thumbnails for draft attachments.
 * Uses Thumbnailator for high-quality resizing.
 */
@Slf4j
@Service
public class ImagePreviewService {

    private static final int THUMB_WIDTH = 200;
    private static final int THUMB_HEIGHT = 200;
    private static final String THUMB_DIR = "thumbnails";

    /**
     * Generate a 200x200 thumbnail for an image file.
     * @return The relative path to the generated thumbnail.
     */
    public String generateThumbnail(Long draftId, String originalFilePath, String fileName) {
        try {
            Path originalPath = Paths.get(originalFilePath);
            if (!Files.exists(originalPath)) {
                log.warn("Original file not found for thumbnail generation: {}", originalFilePath);
                return null;
            }

            // Create thumbnails directory if it doesn't exist
            Path draftDir = originalPath.getParent();
            Path thumbDir = draftDir.resolve(THUMB_DIR);
            if (!Files.exists(thumbDir)) {
                Files.createDirectories(thumbDir);
            }

            String thumbFileName = "thumb_" + fileName;
            Path thumbPath = thumbDir.resolve(thumbFileName);

            log.info("Generating thumbnail for {}: {}", fileName, thumbPath);

            Thumbnails.of(originalPath.toFile())
                    .size(THUMB_WIDTH, THUMB_HEIGHT)
                    .outputFormat("jpg")
                    .toFile(thumbPath.toFile());

            // Return predictable path structure for MediaController
            return "/api/media/preview/" + draftId + "/" + thumbFileName;

        } catch (IOException e) {
            log.error("Failed to generate thumbnail for {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * Helper to check if a file is a support image format
     */
    public boolean isSupportedImage(String fileName) {
        if (fileName == null) return false;
        String ext = fileName.toLowerCase();
        return ext.endsWith(".jpg") || ext.endsWith(".jpeg") || 
               ext.endsWith(".png") || ext.endsWith(".gif");
    }
}

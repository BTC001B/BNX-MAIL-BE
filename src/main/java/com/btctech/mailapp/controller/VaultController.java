package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.model.VaultFile;
import com.btctech.mailapp.repository.VaultFileRepository;
import com.btctech.mailapp.service.UserService;
import com.btctech.mailapp.service.VaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vault")
public class VaultController {

    @Autowired
    private VaultService vaultService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private User getUserFromToken(String token) {
        String jwt = token.replace("Bearer ", "");
        String identifier = jwtUtil.extractEmail(jwt);
        return userService.getUserByEmailOrUsername(identifier);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestHeader("Authorization") String token,
                                        @RequestParam("file") MultipartFile file) {
        try {
            User user = getUserFromToken(token);
            VaultFile vaultFile = vaultService.uploadFile(file, user.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("id", vaultFile.getId());
            response.put("filename", vaultFile.getFilename());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not upload the file: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserFiles(@RequestHeader("Authorization") String token) {
        try {
            User user = getUserFromToken(token);
            List<VaultFileRepository.VaultFileMetadata> files = vaultService.getUserFiles(user.getEmail());
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not retrieve files"));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@RequestHeader("Authorization") String token,
                                          @PathVariable Long id) {
        try {
            User user = getUserFromToken(token);
            VaultFile file = vaultService.getFile(id, user.getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                    .body(file.getData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@RequestHeader("Authorization") String token,
                                        @PathVariable Long id) {
        try {
            User user = getUserFromToken(token);
            vaultService.deleteFile(id, user.getEmail());
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

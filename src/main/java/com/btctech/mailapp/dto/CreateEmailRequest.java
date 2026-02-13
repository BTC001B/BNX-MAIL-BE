package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEmailRequest {
    
    @NotBlank(message = "Email name is required")
    @Size(min = 3, max = 30, message = "Email name must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-z0-9._-]+$", 
             message = "Email name can only contain lowercase letters, numbers, dots, hyphens and underscores")
    private String emailName;
    
    // ✅ Password field (required for temp tokens, optional for regular tokens)
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
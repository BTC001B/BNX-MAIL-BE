package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Account type is required")
    private String accountType; // PUBLIC, BUSINESS, CHILD

    // Business fields
    private String companyName;
    private String domain;

    // Child fields
    private String parentEmail;
    private java.time.LocalDate dob;
}

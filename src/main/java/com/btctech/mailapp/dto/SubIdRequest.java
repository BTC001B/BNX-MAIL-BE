package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubIdRequest {

    @NotBlank(message = "Prefix is required")
    private String prefix;

    @NotBlank(message = "Password is required")
    private String password;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Account Type is required (PERSONAL or BUSINESS)")
    private String accountType; // "PERSONAL" or "BUSINESS"
    
    private java.util.List<Integer> permissions;
}

package com.btctech.mailapp.dto;

import lombok.Data;

@Data
public class InviteMemberRequest {
    private String emailName;
    private String firstName;
    private String lastName;
    private String role = "ORG_USER"; // Default to ORG_USER
}

package com.btctech.mailapp.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private String firstName;
    private String lastName;
    private String nickname;
    private String displayName;
    private String gender;
    private String dob; // date format e.g. YYYY-MM-DD
    private String phoneNumber;
    private String recoveryEmail;
    private String homeAddress;
    private String workAddress;
    private String occupation;
    private String bio;
}

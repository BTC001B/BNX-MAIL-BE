package com.btctech.mailapp.dto;

import lombok.Data;

@Data
public class BusinessOnboardingRequest {
    private String businessType;
    private String industry;
    private String companySize;
    private String businessWebsite;
    private String businessAddress;
    private String timeZone;
    private String language;
    private String profilePhoto;
    private String companyLogo;
    private Boolean acceptTerms;
}

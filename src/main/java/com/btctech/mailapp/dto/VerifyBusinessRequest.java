package com.btctech.mailapp.dto;

import lombok.Data;

@Data
public class VerifyBusinessRequest {
    private String type; // 'CIN' or 'GSTIN'
    private String cin;
    private String pan; // Used for PAN-to-GSTIN
    private String gstin; // Used to verify within PAN-to-GSTIN list
}

package com.btctech.mailapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecoveryOptionsResponse {
    private String recoveryEmail;
    private String phoneNumber;
}

package com.btctech.mailapp.dto.cashfree;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class CashfreeCreateUrlResponse {
    private String status;
    @JsonProperty("verification_id")
    private String verificationId;
    @JsonProperty("url")
    private String redirectUrl;
    @JsonProperty("reference_id")
    private Long referenceId;
}

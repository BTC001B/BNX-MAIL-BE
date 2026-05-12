package com.btctech.mailapp.dto.cashfree;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class CashfreeStatusResponse {
    private String status;
    @JsonProperty("verification_id")
    private String verificationId;
    @JsonProperty("reference_id")
    private String referenceId;
    @JsonProperty("verification_status")
    private String verificationStatus; // PENDING, AUTHENTICATED, EXPIRED, etc.
}

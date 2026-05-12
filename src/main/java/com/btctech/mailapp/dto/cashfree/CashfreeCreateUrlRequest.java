package com.btctech.mailapp.dto.cashfree;

import lombok.Data;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@Builder
public class CashfreeCreateUrlRequest {
    @JsonProperty("verification_id")
    private String verificationId;
    
    @JsonProperty("redirect_url")
    private String redirectUrl;

    @JsonProperty("document_requested")
    private java.util.List<String> documentRequested;

    @JsonProperty("user_flow")
    private String userFlow; // signup or signin
}

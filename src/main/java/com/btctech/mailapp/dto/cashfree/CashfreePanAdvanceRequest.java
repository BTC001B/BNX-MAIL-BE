package com.btctech.mailapp.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashfreePanAdvanceRequest {
    @JsonProperty("pan")
    private String pan;

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("name")
    private String name;
}

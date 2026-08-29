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
public class CashfreePanResponse {

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("type")
    private String type;

    @JsonProperty("reference_id")
    private Long referenceId;

    @JsonProperty("name_provided")
    private String nameProvided;

    @JsonProperty("registered_name")
    private String registeredName;

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("message")
    private String message;
}

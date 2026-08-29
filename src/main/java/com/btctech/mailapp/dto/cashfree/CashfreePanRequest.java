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
public class CashfreePanRequest {

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("name")
    private String name;
}

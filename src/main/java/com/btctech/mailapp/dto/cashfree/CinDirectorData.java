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
public class CinDirectorData {

    @JsonProperty("din")
    private String din;

    @JsonProperty("name")
    private String name;

    @JsonProperty("designation")
    private String designation;
}

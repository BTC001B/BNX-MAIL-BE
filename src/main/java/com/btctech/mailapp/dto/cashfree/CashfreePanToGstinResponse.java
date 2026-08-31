package com.btctech.mailapp.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashfreePanToGstinResponse {

    @JsonProperty("reference_id")
    private Long referenceId;

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("gstin_list")
    private List<GstinData> gstinList;
}

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
public class CashfreeGstinResponse {

    @JsonProperty("reference_id")
    private Long referenceId;
    
    @JsonProperty("GSTIN")
    private String gstin;
    
    @JsonProperty("legal_name_of_business")
    private String legalNameOfBusiness;
    
    @JsonProperty("trade_name_of_business")
    private String tradeNameOfBusiness;
    
    @JsonProperty("date_of_registration")
    private String dateOfRegistration;
    
    @JsonProperty("constitution_of_business")
    private String constitutionOfBusiness;
    
    @JsonProperty("taxpayer_type")
    private String taxpayerType;
    
    @JsonProperty("gst_in_status")
    private String gstInStatus;
    
    @JsonProperty("valid")
    private boolean valid;
    
    @JsonProperty("message")
    private String message;
}

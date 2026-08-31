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
public class CashfreeCinResponse {

    @JsonProperty("reference_id")
    private Long referenceId;

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("cin")
    private String cin;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("registration_number")
    private String registrationNumber;

    @JsonProperty("incorporation_date")
    private String incorporationDate;

    @JsonProperty("cin_status")
    private String cinStatus;

    @JsonProperty("email")
    private String email;

    @JsonProperty("director_details")
    private List<CinDirectorData> directorDetails;
}

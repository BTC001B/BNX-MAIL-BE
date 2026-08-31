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
public class CashfreePanAdvanceResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("reference_id")
    private Long referenceId;

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("name_provided")
    private String nameProvided;

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("registered_name")
    private String registeredName;

    @JsonProperty("name_pan_card")
    private String namePanCard;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    @JsonProperty("masked_aadhaar_number")
    private String maskedAadhaarNumber;

    @JsonProperty("email")
    private String email;

    @JsonProperty("mobile_number")
    private String mobileNumber;

    @JsonProperty("aadhaar_linked")
    private Boolean aadhaarLinked;
}

package com.btctech.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CasboxStatusRequest {
    private List<Long> messageIds;
    
    @NotBlank(message = "Status is required")
    private String status;
}

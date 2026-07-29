package com.btctech.mailapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageQuotaDTO {
    private String email;
    private long storageLimit;
    private long storageUsed;
    private double storagePercentage;
}

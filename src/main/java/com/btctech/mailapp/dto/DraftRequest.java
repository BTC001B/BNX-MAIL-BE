package com.btctech.mailapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DraftRequest {
    private Long id; // Null for new drafts
    private Long mailAccountId;
    private Long userId; // The person currently editing
    private String to;
    private String cc;
    private String bcc;
    private String subject;
    private String body;
    @JsonProperty("isHtml")
    private Boolean isHtml = false;

    @JsonProperty("isHtml")
    public Boolean getIsHtml() {
        return isHtml;
    }

    @JsonProperty("isHtml")
    public void setIsHtml(Boolean isHtml) {
        this.isHtml = isHtml;
    }
}

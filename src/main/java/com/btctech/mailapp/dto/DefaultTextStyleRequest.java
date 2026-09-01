package com.btctech.mailapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultTextStyleRequest {
    private String fontFamily;
    private String fontSize;
    private String textColor;
}

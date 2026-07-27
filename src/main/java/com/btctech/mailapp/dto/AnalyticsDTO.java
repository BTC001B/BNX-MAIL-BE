package com.btctech.mailapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDTO {
    
    // e.g. "Inbox" -> 1500, "Sent" -> 300, etc.
    private Map<String, Integer> folderCounts;
    
    // e.g. "2023-10-15" -> 45
    private Map<String, Integer> receivedByDate;
    private Map<String, Integer> sentByDate;
    
    // e.g. "2023-10" -> 1450
    private Map<String, Integer> receivedByMonth;
    private Map<String, Integer> sentByMonth;
    
    // e.g. "john@example.com" -> 120
    private Map<String, Integer> topSenders;
    private Map<String, Integer> topReceivers;

}

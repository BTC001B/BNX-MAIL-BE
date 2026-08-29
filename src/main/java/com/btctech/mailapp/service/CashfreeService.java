package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.cashfree.CashfreeCreateUrlRequest;
import com.btctech.mailapp.dto.cashfree.CashfreeCreateUrlResponse;
import com.btctech.mailapp.dto.cashfree.CashfreeStatusResponse;
import com.btctech.mailapp.dto.cashfree.CashfreePanRequest;
import com.btctech.mailapp.dto.cashfree.CashfreePanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;

@Slf4j
@Service
public class CashfreeService {

    @Value("${cashfree.client-id}")
    private String clientId;

    @Value("${cashfree.client-secret}")
    private String clientSecret;

    @Value("${cashfree.api-base-url:https://sandbox.cashfree.com/verification}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public CashfreeCreateUrlResponse createDigiLockerUrl(String referenceId, String redirectUrl) {
        String url = apiBaseUrl + "/digilocker";

        CashfreeCreateUrlRequest request = CashfreeCreateUrlRequest.builder()
                .verificationId(referenceId)
                .redirectUrl(redirectUrl)
                .documentRequested(Collections.singletonList("AADHAAR"))
                .userFlow("signup")
                .build();

        HttpHeaders headers = createHeaders();
        HttpEntity<CashfreeCreateUrlRequest> entity = new HttpEntity<>(request, headers);

        log.info("Calling Cashfree Create URL for reference: {}", referenceId);
        try {
            ResponseEntity<CashfreeCreateUrlResponse> response = restTemplate.postForEntity(url, entity, CashfreeCreateUrlResponse.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to create DigiLocker URL: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Cashfree: {}", e.getMessage());
            throw new RuntimeException("Cashfree API Error: " + e.getMessage());
        }
    }

    public CashfreeStatusResponse getVerificationStatus(String verificationId) {
        String url = apiBaseUrl + "/digilocker?verification_id=" + verificationId;

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CashfreeStatusResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, CashfreeStatusResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error checking Cashfree status: {}", e.getMessage());
            throw new RuntimeException("Failed to check status: " + e.getMessage());
        }
    }

    public CashfreePanResponse verifyPan(String pan, String name) {
        String url = apiBaseUrl + "/pan";

        CashfreePanRequest request = CashfreePanRequest.builder()
                .pan(pan)
                .name(name)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-client-secret", clientSecret);
        headers.set("x-api-version", "2023-12-18"); // Version required for PAN Verification

        HttpEntity<CashfreePanRequest> entity = new HttpEntity<>(request, headers);

        log.info("Calling Cashfree PAN Verification for PAN: {}", pan);
        try {
            ResponseEntity<CashfreePanResponse> response = restTemplate.postForEntity(url, entity, CashfreePanResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Cashfree PAN API: {}", e.getMessage());
            throw new RuntimeException("Cashfree API Error: " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-client-secret", clientSecret);
        return headers;
    }
}

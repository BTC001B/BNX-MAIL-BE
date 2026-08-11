package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.IncomingMailWebhookRequest;
import com.btctech.mailapp.service.PushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class WebhookController {

    private final PushNotificationService pushNotificationService;

    // Use a fixed token for internal webhook authentication.
    // In application.properties: app.webhook.secret=some_secure_random_string
    @Value("${app.webhook.secret:secure-internal-webhook-token-2026}")
    private String webhookSecret;

    public WebhookController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/incoming-mail")
    public ResponseEntity<ApiResponse<String>> handleIncomingMail(
            @RequestHeader(value = "X-Webhook-Token", required = false) String token,
            @RequestBody IncomingMailWebhookRequest request) {

        if (token == null || !token.equals(webhookSecret)) {
            log.warn("Unauthorized webhook attempt with token: {}", token);
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Invalid or missing webhook token."));
        }

        if (request.getToEmail() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing 'toEmail' in request."));
        }

        log.info("Webhook received for incoming email to: {}", request.getToEmail());

        // Fire off push notification in a non-blocking manner or directly
        pushNotificationService.sendNewEmailNotification(
                request.getToEmail(),
                request.getFromEmail(),
                request.getSubject()
        );

        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed successfully."));
    }
}

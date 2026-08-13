package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.UserDevice;
import com.btctech.mailapp.repository.UserDeviceRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PushNotificationService {

    private final UserDeviceRepository userDeviceRepository;

    public PushNotificationService(UserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    /**
     * Sends a push notification to all registered devices of a user.
     * @param userEmail The recipient's email address
     * @param senderName The name or email of the person who sent the email
     * @param subject The subject line of the new email
     */
    public void sendNewEmailNotification(String userEmail, String senderName, String subject) {
        try {
            List<UserDevice> devices = userDeviceRepository.findByUserEmail(userEmail);
            
            if (devices.isEmpty()) {
                log.info("No registered devices found for push notification to {}", userEmail);
                return;
            }

            for (UserDevice device : devices) {
                try {
                    Notification notification = Notification.builder()
                            .setTitle(senderName != null ? senderName : "Unknown")
                            .setBody(subject != null ? subject : "No subject")
                            .build();

                    AndroidConfig androidConfig = AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("bnx_mail_new_email")
                                    .build())
                            .build();

                    Message message = Message.builder()
                            .setToken(device.getDeviceToken())
                            .setNotification(notification)
                            .putData("type", "new_email")
                            .putData("emailId", "0") // Update if a specific email ID is passed via webhook
                            .putData("accountEmail", userEmail)
                            .setAndroidConfig(androidConfig)
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);
                    log.info("Successfully sent push notification to device {}: {}", device.getDeviceToken().substring(0, 8) + "...", response);
                } catch (Exception e) {
                    log.error("Failed to send push notification to device token {}: {}", device.getDeviceToken(), e.getMessage());
                    // Consider deleting invalid tokens if Firebase throws an invalid registration token error
                }
            }
        } catch (Exception e) {
            log.error("Error processing push notifications for {}: {}", userEmail, e.getMessage());
        }
    }
}

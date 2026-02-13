package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.exception.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {
    
    @Value("${mail.smtp.host}")
    private String smtpHost;
    
    @Value("${mail.smtp.port}")
    private int smtpPort;
    
    private final SessionService sessionService;
    
    /**
     * Send email - Password retrieved from session automatically
     */
    public void sendMail(String fromEmail, String password, SendMailRequest request) {
        
        log.info("Attempting to send email from {} to {}", fromEmail, request.getTo());
        
        // Validate inputs
        if (password == null || password.isEmpty()) {
            throw new MailException("Password not found in session");
        }
        
        try {
            // SMTP Properties
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.timeout", "10000"); // 10 seconds
            props.put("mail.smtp.connectiontimeout", "10000");
            
            log.debug("SMTP Config: host={}, port={}", smtpHost, smtpPort);
            
            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    log.debug("Authenticating as: {}", fromEmail);
                    return new PasswordAuthentication(fromEmail, password);
                }
            });
            
            // Enable debug mode (optional, remove in production)
            session.setDebug(false);
            
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getTo()));
            
            // Add CC if present
            if (request.getCc() != null && !request.getCc().isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(request.getCc()));
            }
            
            // Add BCC if present
            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(request.getBcc()));
            }
            
            message.setSubject(request.getSubject());
            
            // Set body (HTML or plain text)
            if (request.getIsHtml() != null && request.getIsHtml()) {
                message.setContent(request.getBody(), "text/html; charset=utf-8");
            } else {
                message.setText(request.getBody(), "utf-8");
            }
            
            // Send message
            log.info("Sending email to SMTP server...");
            Transport.send(message);
            
            log.info("✓ Email sent successfully from {} to {}", fromEmail, request.getTo());
            
        } catch (MessagingException e) {
            log.error("Failed to send email from {} to {}: {}", fromEmail, request.getTo(), e.getMessage(), e);
            throw new MailException("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage(), e);
            throw new MailException("Unexpected error: " + e.getMessage());
        }
    }
}
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
            props.put("mail.smtp.auth", "false"); // Local Postfix on 127.0.0.1 — no auth needed
            props.put("mail.smtp.starttls.enable", "false"); // No TLS for localhost
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.timeout", "10000"); // 10 seconds
            props.put("mail.smtp.connectiontimeout", "10000");
            
            log.debug("SMTP Config: host={}, port={}", smtpHost, smtpPort);
            
            // Create session with authentication
            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
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
            
            // Handle Multipart (Body + Attachments)
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                Multipart multipart = new jakarta.mail.internet.MimeMultipart();

                // 1. Add Text/HTML Body Part
                jakarta.mail.internet.MimeBodyPart messageBodyPart = new jakarta.mail.internet.MimeBodyPart();
                if (request.getIsHtml() != null && request.getIsHtml()) {
                    messageBodyPart.setContent(request.getBody(), "text/html; charset=utf-8");
                } else {
                    messageBodyPart.setText(request.getBody(), "utf-8");
                }
                multipart.addBodyPart(messageBodyPart);

                // 2. Add Attachments
                for (com.btctech.mailapp.dto.AttachmentInfo attachment : request.getAttachments()) {
                    jakarta.mail.internet.MimeBodyPart attachPart = new jakarta.mail.internet.MimeBodyPart();
                    try {
                        attachPart.attachFile(new java.io.File(attachment.getFilePath()));
                        attachPart.setFileName(attachment.getFileName());
                        multipart.addBodyPart(attachPart);
                    } catch (java.io.IOException ex) {
                        log.error("Failed to attach file: {}", attachment.getFileName());
                        // Continue sending even if one attachment fails
                    }
                }

                message.setContent(multipart);
            } else {
                // Standard single-part message
                if (request.getIsHtml() != null && request.getIsHtml()) {
                    message.setContent(request.getBody(), "text/html; charset=utf-8");
                } else {
                    message.setText(request.getBody(), "utf-8");
                }
            }
            
            // Send message
            log.info("Sending email to SMTP server...");
            Transport.send(message);
            
            log.info("✓ Email sent successfully from {} to {}", fromEmail, request.getTo());
            
            // 10/10 Polish: Save copy to "Sent" folder in IMAP
            saveCopyToSent(fromEmail, password, message);
            
        } catch (MessagingException e) {
            log.error("Failed to send email from {} to {}: {}", fromEmail, request.getTo(), e.getMessage(), e);
            throw new MailException("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage(), e);
            throw new MailException("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Save a copy of the message to the Sent folder using IMAP
     */
    private void saveCopyToSent(String email, String password, MimeMessage message) {
        log.debug("Saving copy to 'Sent' folder for {}", email);
        Store store = null;
        Folder sentFolder = null;

        try {
            Properties props = new Properties();
            // Use IMAP settings from properties or shared config
            // Note: Since we don't have direct access to MailReceiveService's private fields, 
            // we redeclare them or ideally should have a shared MailConfiguration.
            // For now, we'll use same logic as MailReceiveService.
            props.put("mail.imap.host", "localhost"); // Hardcoded to localhost as per properties
            props.put("mail.imap.port", "993");
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.trust", "*");

            Session session = Session.getInstance(props);
            store = session.getStore("imap");
            store.connect("localhost", 993, email, password);

            sentFolder = store.getFolder("Sent");
            if (!sentFolder.exists()) {
                log.info("Sent folder does not exist, creating it...");
                sentFolder.create(Folder.HOLDS_MESSAGES);
            }

            sentFolder.open(Folder.READ_WRITE);
            
            // Set SEEN flag for sent mail
            message.setFlag(Flags.Flag.SEEN, true);
            
            sentFolder.appendMessages(new Message[]{message});
            log.info("✓ Message archived to 'Sent' folder for {}", email);

        } catch (Exception e) {
            log.warn("Failed to save copy to 'Sent' folder: {}. Email was still sent.", e.getMessage());
        } finally {
            try {
                if (sentFolder != null && sentFolder.isOpen()) sentFolder.close(false);
                if (store != null) store.close();
            } catch (MessagingException e) {
                // Ignore close errors
            }
        }
    }
}
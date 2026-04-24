package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.dto.AttachmentInfo;
import com.btctech.mailapp.exception.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {
    
    private final MailReceiveService mailReceiveService;

    @Value("${mail.smtp.host}")
    private String smtpHost;

    @Value("${mail.smtp.port}")
    private int smtpPort;

    @Value("${mail.imap.host}")
    private String imapHost;

    @Value("${mail.imap.port}")
    private int imapPort;

    /**
     * Send email - Password retrieved from session automatically
     */
    public void sendMail(String fromEmail, String password, SendMailRequest request) {
        
        log.info("Attempting to send email from {} to {}", fromEmail, request.getTo());
        
        if (password == null || password.isEmpty()) {
            throw new MailException("Password not found in session");
        }
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "false"); // Local Postfix on 127.0.0.1
            props.put("mail.smtp.starttls.enable", "false"); 
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.timeout", "10000"); 
            props.put("mail.smtp.connectiontimeout", "10000");
            
            log.debug("SMTP Config: host={}, port={}", smtpHost, smtpPort);
            
            Session session = Session.getInstance(props);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getTo()));
            
            if (request.getCc() != null && !request.getCc().isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(request.getCc()));
            }
            
            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(request.getBcc()));
            }
            
            message.setSubject(request.getSubject());
            
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                Multipart multipart = new MimeMultipart();

                // 1. Add Text/HTML Body Part
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                if (request.getIsHtml() != null && request.getIsHtml()) {
                    messageBodyPart.setContent(request.getBody(), "text/html; charset=utf-8");
                } else {
                    messageBodyPart.setText(request.getBody(), "utf-8");
                }
                multipart.addBodyPart(messageBodyPart);

                // 2. Add Attachments
                for (AttachmentInfo attachment : request.getAttachments()) {
                    MimeBodyPart attachPart = new MimeBodyPart();
                    try {
                        attachPart.attachFile(new File(attachment.getFilePath()));
                        attachPart.setFileName(attachment.getFileName());
                        multipart.addBodyPart(attachPart);
                    } catch (IOException ex) {
                        log.error("Failed to attach file: {}", attachment.getFileName());
                    }
                }

                message.setContent(multipart);
            } else {
                if (request.getIsHtml() != null && request.getIsHtml()) {
                    message.setContent(request.getBody(), "text/html; charset=utf-8");
                } else {
                    message.setText(request.getBody(), "utf-8");
                }
            }
            
            log.info("Sending email to SMTP server...");
            Transport.send(message);
            
            log.info("✓ Email sent successfully from {} to {}", fromEmail, request.getTo());
            
            // Archival process (IMAP "Sent" folder)
            saveCopyToSent(fromEmail, password, message);
            
        } catch (MessagingException e) {
            log.error("Failed to send email from {} to {}: {}", fromEmail, request.getTo(), e.getMessage());
            throw new MailException("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage());
            throw new MailException("Unexpected error: " + e.getMessage());
        }
    }

    private void saveCopyToSent(String email, String password, MimeMessage message) {
        log.info("Starting archival process for sent email from {}", email);
        Store store = null;
        Folder sentFolder = null;

        try {
            store = mailReceiveService.connect(email, password);
            log.info("✓ Connected to IMAP for archival on {}:{}", imapHost, imapPort);
            String actualSentFolder = mailReceiveService.resolveSentFolderName(store);
            log.info("Resolved Sent folder name: {}", actualSentFolder);

            sentFolder = store.getFolder(actualSentFolder);
            if (!sentFolder.exists()) {
                log.info("Sent folder '{}' does not exist, attempting to create it...", actualSentFolder);
                sentFolder.create(Folder.HOLDS_MESSAGES);
            }

            sentFolder.open(Folder.READ_WRITE);
            
            MimeMessage copy = new MimeMessage(message);
            copy.setFlag(Flags.Flag.SEEN, true);
            copy.saveChanges();
            
            sentFolder.appendMessages(new Message[]{copy});
            log.info("✓ Message successfully archived to '{}' folder for {}", actualSentFolder, email);

        } catch (Exception e) {
            log.warn("⚠ SENT ARCHIVAL WARNING: Failed to archive copy for {}. Reason: {}", email, e.getMessage());
        } finally {
            try {
                if (sentFolder != null && sentFolder.isOpen()) sentFolder.close(false);
                if (store != null) store.close();
            } catch (MessagingException e) {
                log.debug("Error closing IMAP resources: {}", e.getMessage());
            }
        }
    }
}
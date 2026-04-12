package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.EmailDTO;
import com.btctech.mailapp.exception.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailReceiveService {

    @Value("${mail.imap.host}")
    private String imapHost;

    @Value("${mail.imap.port}")
    private int imapPort;

    /**
     * Get inbox emails
     */
    public List<EmailDTO> getInbox(String email, String password, int limit) {
        log.info("Fetching inbox for: {}", email);

        Store store = null;
        Folder inbox = null;

        try {
            // IMAP Properties
            Properties props = new Properties();
            props.put("mail.imap.host", imapHost);
            props.put("mail.imap.port", String.valueOf(imapPort));
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.imap.ssl.trust", "*"); // Trust all certificates for local dev
            props.put("mail.imap.connectiontimeout", "10000");
            props.put("mail.imap.timeout", "10000");

            // Create session
            Session session = Session.getInstance(props);

            // Connect to store
            store = session.getStore("imap");
            store.connect(imapHost, imapPort, email, password);

            log.debug("Connected to IMAP server");

            // Open inbox folder
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int messageCount = inbox.getMessageCount();
            log.info("Total messages in inbox: {}", messageCount);

            if (messageCount == 0) {
                return new ArrayList<>();
            }

            // Get messages (most recent first)
            int start = Math.max(1, messageCount - limit + 1);
            Message[] messages = inbox.getMessages(start, messageCount);

            // Reverse to get newest first
            List<Message> messageList = Arrays.asList(messages);
            List<Message> reversedList = new ArrayList<>(messageList);
            java.util.Collections.reverse(reversedList);

            // Convert to DTO
            List<EmailDTO> emails = new ArrayList<>();
            for (Message message : reversedList) {
                try {
                    EmailDTO emailDTO = convertToDTO(message);
                    emails.add(emailDTO);
                } catch (Exception e) {
                    log.warn("Failed to parse message: {}", e.getMessage());
                }
            }

            log.info("Fetched {} emails", emails.size());
            return emails;

        } catch (MessagingException e) {
            log.error("Failed to fetch inbox: {}", e.getMessage(), e);
            throw new MailException("Failed to fetch inbox: " + e.getMessage());
        } finally {
            // Close connections
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                log.warn("Error closing IMAP connection: {}", e.getMessage());
            }
        }
    }

    /**
     * Get single email by UID
     */
    public EmailDTO getEmail(String email, String password, String uid) {
        // Implementation for getting single email
        // For now, just get all and filter
        List<EmailDTO> emails = getInbox(email, password, 100);
        return emails.stream()
                .filter(e -> e.getUid().equals(uid))
                .findFirst()
                .orElseThrow(() -> new MailException("Email not found"));
    }

    /**
     * Convert Message to EmailDTO
     */
    private EmailDTO convertToDTO(Message message) throws MessagingException, IOException {
        EmailDTO dto = new EmailDTO();

        // UID (use message number as UID for now)
        dto.setUid(String.valueOf(message.getMessageNumber()));

        // From
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            dto.setFrom(((InternetAddress) fromAddresses[0]).getAddress());
        }

        // To
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null && toAddresses.length > 0) {
            dto.setTo(((InternetAddress) toAddresses[0]).getAddress());
        }

        // Subject
        dto.setSubject(message.getSubject());

        // Dates
        dto.setSentDate(message.getSentDate());
        dto.setReceivedDate(message.getReceivedDate());

        // Read status
        dto.setRead(message.isSet(Flags.Flag.SEEN));

        // Size
        dto.setSize(message.getSize());

        // Body
        String[] content = extractContent(message);
        dto.setBody(content[0]); // Plain text
        dto.setHtmlBody(content[1]); // HTML

        // Attachments
        dto.setHasAttachments(hasAttachments(message));

        return dto;
    }

    /**
     * Extract email content (text and HTML)
     */
    private String[] extractContent(Message message) throws MessagingException, IOException {
        String plainText = "";
        String html = "";

        Object content = message.getContent();

        if (content instanceof String) {
            plainText = (String) content;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                if (bodyPart.isMimeType("text/plain")) {
                    plainText = (String) bodyPart.getContent();
                } else if (bodyPart.isMimeType("text/html")) {
                    html = (String) bodyPart.getContent();
                } else if (bodyPart.getContent() instanceof MimeMultipart) {
                    // Nested multipart
                    MimeMultipart nested = (MimeMultipart) bodyPart.getContent();
                    for (int j = 0; j < nested.getCount(); j++) {
                        BodyPart nestedPart = nested.getBodyPart(j);
                        if (nestedPart.isMimeType("text/plain")) {
                            plainText = (String) nestedPart.getContent();
                        } else if (nestedPart.isMimeType("text/html")) {
                            html = (String) nestedPart.getContent();
                        }
                    }
                }
            }
        }

        // If no plain text but has HTML, convert HTML to plain text (simple)
        if (plainText.isEmpty() && !html.isEmpty()) {
            plainText = html.replaceAll("<[^>]*>", "").trim();
        }

        return new String[] { plainText, html };
    }

    /**
     * Check if message has attachments
     */
    private boolean hasAttachments(Message message) throws MessagingException, IOException {
        Object content = message.getContent();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get unread count
     */
    public int getUnreadCount(String email, String password) {
        Store store = null;
        Folder inbox = null;

        try {
            Properties props = new Properties();
            props.put("mail.imap.host", imapHost);
            props.put("mail.imap.port", String.valueOf(imapPort));
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.trust", "*"); // Trust all certificates for local dev

            Session session = Session.getInstance(props);
            store = session.getStore("imap");
            store.connect(imapHost, imapPort, email, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            return inbox.getUnreadMessageCount();

        } catch (MessagingException e) {
            log.error("Failed to get unread count: {}", e.getMessage());
            return 0;
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                log.warn("Error closing connection: {}", e.getMessage());
            }
        }
    }

    /**
     * Mark email as read
     */
    public void markAsRead(String email, String password, String uid) {
        Store store = null;
        Folder inbox = null;

        try {
            // IMAP Properties
            Properties props = new Properties();
            props.put("mail.imap.host", imapHost);
            props.put("mail.imap.port", String.valueOf(imapPort));
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.imap.ssl.trust", "*"); // Trust all certificates for local dev
            props.put("mail.imap.connectiontimeout", "10000");
            props.put("mail.imap.timeout", "10000");

            Session session = Session.getInstance(props);

            store = session.getStore("imap");
            store.connect(imapHost, imapPort, email, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE); // Open in READ_WRITE mode

            int msgNum;
            try {
                msgNum = Integer.parseInt(uid);
            } catch (NumberFormatException e) {
                log.warn("Invalid UID format: {}", uid);
                return;
            }

            try {
                Message message = inbox.getMessage(msgNum);
                message.setFlag(Flags.Flag.SEEN, true);
                log.info("Marked message {} as read for {}", uid, email);
            } catch (IndexOutOfBoundsException e) {
                log.warn("Message {} not found for {}", uid, email);
            }

        } catch (MessagingException e) {
            log.error("Failed to mark as read: {}", e.getMessage(), e);
            throw new MailException("Failed to mark as read");
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                log.warn("Error closing IMAP connection: {}", e.getMessage());
            }
        }
    }
}
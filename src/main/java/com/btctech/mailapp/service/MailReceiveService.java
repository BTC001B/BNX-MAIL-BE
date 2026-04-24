package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.StarredEmail;
import com.btctech.mailapp.entity.SnoozedEmail;
import com.btctech.mailapp.repository.StarredEmailRepository;
import com.btctech.mailapp.repository.SnoozedEmailRepository;
import com.btctech.mailapp.repository.MailLabelMappingRepository;
import com.btctech.mailapp.entity.MailLabelMapping;


import com.btctech.mailapp.dto.EmailDTO;
import com.btctech.mailapp.exception.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
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

    @Value("${mail.imap.protocol:imap}")
    private String imapProtocol;

    @Value("${mail.imap.ssl.enable:false}")
    private boolean imapSslEnable;

    private final StarredEmailRepository starredEmailRepository;
    private final SnoozedEmailRepository snoozedEmailRepository;
    private final MailLabelMappingRepository labelMappingRepository;



    /**
     * Common method to fetch emails from a specific folder
     */
    private List<EmailDTO> getEmailsFromFolder(String email, String password, String folderName, int limit) {
        log.info("Fetching messages from folder '{}' for: {}", folderName, email);

        Store store = null;
        Folder folder = null;

        try {
            store = connect(email, password);

            String actualFolderName = folderName;
            if ("Sent".equalsIgnoreCase(folderName)) {
                actualFolderName = resolveSentFolderName(store);
            } else if ("Trash".equalsIgnoreCase(folderName)) {
                actualFolderName = resolveTrashFolderName(store);
            } else if ("Spam".equalsIgnoreCase(folderName)) {
                actualFolderName = resolveSpamFolderName(store);
            } else if ("Snoozed".equalsIgnoreCase(folderName)) {
                actualFolderName = resolveSnoozedFolderName(store);
            }



            folder = store.getFolder(actualFolderName);
            log.info("Attempting to open folder: '{}' (Resolved from: '{}')", actualFolderName, folderName);
            
            if (!folder.exists()) {
                log.warn("⚠ FOLDER MISSING: '{}' does not exist for user {}. Returning empty list.", actualFolderName, email);
                return new ArrayList<>();
            }
            
            folder.open(Folder.READ_ONLY);
            log.info("Successfully opened '{}'. Message count: {}", actualFolderName, folder.getMessageCount());

            if (!(folder instanceof UIDFolder)) {
                log.error("Folder {} does not support UIDs, falling back to message numbers (NOT RECOMMENDED)", actualFolderName);
            }

            UIDFolder uidFolder = (folder instanceof UIDFolder) ? (UIDFolder) folder : null;
            int messageCount = folder.getMessageCount();

            if (messageCount == 0) return new ArrayList<>();

            int start = Math.max(1, messageCount - limit + 1);
            Message[] messages = folder.getMessages(start, messageCount);

            List<EmailDTO> emails = new ArrayList<>();
            for (int i = messages.length - 1; i >= 0; i--) {
                try {
                    emails.add(convertToDTO(messages[i], uidFolder, email));
                } catch (Exception e) {
                    log.warn("Failed to parse message in {}: {}", folderName, e.getMessage());
                }
            }

            return emails;

        } catch (MessagingException e) {
            log.error("Failed to fetch folder {}: {}", folderName, e.getMessage(), e);
            throw new MailException("Failed to fetch " + folderName + ": " + e.getMessage());
        } finally {
            cleanup(store, folder);
        }
    }

    public List<EmailDTO> getInbox(String email, String password, int limit) {
        return getEmailsFromFolder(email, password, "INBOX", limit);
    }

    public List<EmailDTO> getSent(String email, String password, int limit) {
        return getEmailsFromFolder(email, password, "Sent", limit);
    }

    public List<EmailDTO> getTrash(String email, String password, int limit) {
        return getEmailsFromFolder(email, password, "Trash", limit);
    }

    public List<EmailDTO> getSpam(String email, String password, int limit) {
        return getEmailsFromFolder(email, password, "Spam", limit);
    }

    public List<EmailDTO> getSnoozed(String email, String password, int limit) {
        return getEmailsFromFolder(email, password, "Snoozed", limit);
    }

    public List<EmailDTO> getEmailsByCategory(String email, String password, String category, int limit) {
        log.info("Fetching emails for category '{}' for: {}", category, email);
        
        // Strategy: Fetch a larger batch from Inbox and filter by calculated category
        // Since categorization is done on-the-fly, we need to scan the latest messages
        int scanLimit = Math.max(limit * 4, 100); 
        List<EmailDTO> allInbox = getInbox(email, password, scanLimit);
        
        return allInbox.stream()
                .filter(e -> {
                    if ("UNREAD".equalsIgnoreCase(category)) {
                        return !e.isRead();
                    }
                    return category.equalsIgnoreCase(e.getCategory());
                })
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }



    public List<EmailDTO> getEmailsByLabel(String email, String password, Long labelId) {
        log.info("Fetching emails for label ID {} for: {}", labelId, email);
        
        List<MailLabelMapping> mappings = labelMappingRepository.findByUserEmailAndLabelId(email, labelId);
        if (mappings.isEmpty()) return new ArrayList<>();

        jakarta.mail.Store store = null;
        try {
            store = connect(email, password);
            List<EmailDTO> labeledEmails = new ArrayList<>();
            
            for (MailLabelMapping mapping : mappings) {
                try {
                    jakarta.mail.Folder f = store.getFolder(mapping.getFolderName());
                    if (f.exists()) {
                        if (!f.isOpen()) f.open(jakarta.mail.Folder.READ_ONLY);
                        
                        if (f instanceof jakarta.mail.UIDFolder) {
                            jakarta.mail.UIDFolder uidFolder = (jakarta.mail.UIDFolder) f;
                            jakarta.mail.Message msg = uidFolder.getMessageByUID(Long.parseLong(mapping.getEmailUid()));
                            if (msg != null) {
                                labeledEmails.add(convertToDTO(msg, uidFolder, email));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch labeled email for UID {} in {}: {}", mapping.getEmailUid(), mapping.getFolderName(), e.getMessage());
                }
            }
            return labeledEmails;
        } catch (jakarta.mail.MessagingException e) {
            log.error("Failed to connect for labeled emails: {}", e.getMessage());
            throw new MailException("Failed to fetch labeled emails.");
        } finally {
            cleanup(store, null);
        }
    }

    public List<EmailDTO> getStarred(String email, String password, int limit) {
        log.info("Fetching starred messages from local DB for: {}", email);
        
        // 1. Get starred mappings from DB
        List<StarredEmail> starredMappings = starredEmailRepository.findByUserEmail(email);
        if (starredMappings.isEmpty()) return new ArrayList<>();

        Store store = null;
        try {
            store = connect(email, password);
            List<EmailDTO> starredEmails = new ArrayList<>();
            
            // 2. Fetch messages from IMAP using UIDs stored in DB
            for (StarredEmail mapping : starredMappings) {
                try {
                    Folder f = store.getFolder(mapping.getFolderName());
                    if (f.exists()) {
                        if (!f.isOpen()) f.open(Folder.READ_ONLY);
                        
                        if (f instanceof UIDFolder) {
                            UIDFolder uidFolder = (UIDFolder) f;
                            Message msg = uidFolder.getMessageByUID(Long.parseLong(mapping.getUid()));
                            if (msg != null) {
                                EmailDTO dto = convertToDTO(msg, uidFolder, email);
                                dto.setStarred(true); 
                                starredEmails.add(dto);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch starred email for UID {} in {}: {}", mapping.getUid(), mapping.getFolderName(), e.getMessage());
                }
            }
            return starredEmails;
        } catch (MessagingException e) {
            log.error("Failed to connect for starred emails: {}", e.getMessage());
            throw new MailException("Failed to fetch starred emails.");
        } finally {
            cleanup(store, null);
        }
    }

    @Transactional
    public void toggleStar(String email, String password, String folderName, String uid) {
        log.info("Toggling star for email UID {} in folder {} from local DB mapping", uid, folderName);
        String normalizedFolder = folderName.toUpperCase();
        
        var existing = starredEmailRepository.findByUserEmailAndUidAndFolderName(email, uid, normalizedFolder);
        
        if (existing.isPresent()) {
            starredEmailRepository.deleteByUserEmailAndUidAndFolderName(email, uid, normalizedFolder);
            log.info("Successfully UNSTARRED UID {} in {}", uid, normalizedFolder);
        } else {
            StarredEmail star = StarredEmail.builder()
                .userEmail(email)
                .uid(uid)
                .folderName(normalizedFolder)
                .starredAt(java.time.LocalDateTime.now())
                .build();
            starredEmailRepository.save(star);
            log.info("Successfully STARRED UID {} in {}", uid, normalizedFolder);
        }
    }

    public void moveToTrash(String email, String password, String sourceFolderName, String uid) {
        log.info("Moving email UID {} from {} to Trash for {}", uid, sourceFolderName, email);
        moveMessage(email, password, sourceFolderName, uid, "Trash");
    }

    public void markAsSpam(String email, String password, String sourceFolderName, String uid) {
        log.info("Marking email UID {} from {} as SPAM for {}", uid, sourceFolderName, email);
        moveMessage(email, password, sourceFolderName, uid, "Spam");
    }

    @Transactional
    public void snoozeEmail(String email, String password, String sourceFolder, String uid, java.time.LocalDateTime wakeUpAt) {
        log.info("Snoozing email UID {} from {} until {}", uid, sourceFolder, wakeUpAt);
        
        moveMessage(email, password, sourceFolder, uid, "Snoozed");
        
        SnoozedEmail snooze = SnoozedEmail.builder()
            .userEmail(email)
            .uid(uid)
            .originalFolderName(sourceFolder)
            .wakeUpAt(wakeUpAt)
            .build();
            
        snoozedEmailRepository.save(snooze);
    }


    public void moveMessage(String email, String password, String sourceFolderName, String uid, String targetFolderAlias) {
        Store store = null;
        Folder source = null;
        Folder target = null;

        try {
            store = connect(email, password);

            String resolvedTargetFolderName;
            if ("Trash".equalsIgnoreCase(targetFolderAlias)) {
                resolvedTargetFolderName = resolveTrashFolderName(store);
            } else if ("Spam".equalsIgnoreCase(targetFolderAlias)) {
                resolvedTargetFolderName = resolveSpamFolderName(store);
            } else if ("Snoozed".equalsIgnoreCase(targetFolderAlias)) {
                resolvedTargetFolderName = resolveSnoozedFolderName(store);
            } else {
                resolvedTargetFolderName = targetFolderAlias;
            }


            target = store.getFolder(resolvedTargetFolderName);
            if (!target.exists()) target.create(Folder.HOLDS_MESSAGES);

            source = store.getFolder(sourceFolderName);
            source.open(Folder.READ_WRITE);

            if (!(source instanceof UIDFolder)) {
                throw new MailException("Source folder " + sourceFolderName + " does not support persistent UIDs.");
            }
            UIDFolder uidSource = (UIDFolder) source;

            long numericUid = Long.parseLong(uid);
            Message message = uidSource.getMessageByUID(numericUid);

            if (message == null) {
                throw new MailException("Email with UID " + uid + " no longer exists in " + sourceFolderName);
            }

            source.copyMessages(new Message[]{message}, target);
            message.setFlag(Flags.Flag.DELETED, true);
            source.expunge();

            log.info("Successfully moved message {} to {}", uid, resolvedTargetFolderName);

        } catch (Exception e) {
            log.error("Failed to move message to {}: {}", targetFolderAlias, e.getMessage(), e);
            throw new MailException("Failed to move message: " + e.getMessage());
        } finally {
            try { if (source != null && source.isOpen()) source.close(true); } catch (Exception e) {}
            try { if (store != null) store.close(); } catch (Exception e) {}
        }
    }


    public void restoreFromTrash(String email, String password, String uid) {
        log.info("Restoring email UID {} from Trash to Inbox for {}", uid, email);
        restoreFromFolder(email, password, uid, "Trash");
    }

    public void restoreFromSpam(String email, String password, String uid) {
        log.info("Restoring email UID {} from Spam to Inbox for {}", uid, email);
        restoreFromFolder(email, password, uid, "Spam");
    }

    private void restoreFromFolder(String email, String password, String uid, String sourceAlias) {
        Store store = null;
        Folder source = null;
        Folder inbox = null;

        try {
            store = connect(email, password);

            String solvedSource;
            if ("Trash".equalsIgnoreCase(sourceAlias)) solvedSource = resolveTrashFolderName(store);
            else if ("Spam".equalsIgnoreCase(sourceAlias)) solvedSource = resolveSpamFolderName(store);
            else solvedSource = sourceAlias;

            source = store.getFolder(solvedSource);
            source.open(Folder.READ_WRITE);

            if (!(source instanceof UIDFolder)) {
                throw new MailException(sourceAlias + " folder does not support persistent UIDs.");
            }
            UIDFolder uidSource = (UIDFolder) source;

            inbox = store.getFolder("INBOX");
            long numericUid = Long.parseLong(uid);
            Message message = uidSource.getMessageByUID(numericUid);

            if (message == null) {
                throw new MailException("Email with UID " + uid + " no longer exists in " + sourceAlias);
            }

            source.copyMessages(new Message[]{message}, inbox);
            message.setFlag(Flags.Flag.DELETED, true);
            source.expunge();

            log.info("Successfully restored message {} from {} to Inbox", uid, sourceAlias);

        } catch (Exception e) {
            log.error("Failed to restore from {}: {}", sourceAlias, e.getMessage(), e);
            throw new MailException("Failed to restore from " + sourceAlias + ": " + e.getMessage());
        } finally {
            try { if (source != null && source.isOpen()) source.close(true); } catch (Exception e) {}
            try { if (store != null) store.close(); } catch (Exception e) {}
        }
    }

    public void deletePermanently(String email, String password, String uid) {
        log.info("Permanently deleting email UID {} from Trash for {}", uid, email);
        Store store = null;
        Folder trash = null;

        try {
            store = connect(email, password);

            String trashName = resolveTrashFolderName(store);
            trash = store.getFolder(trashName);
            trash.open(Folder.READ_WRITE);

            if (!(trash instanceof UIDFolder)) {
                throw new MailException("Trash folder does not support persistent UIDs.");
            }
            UIDFolder uidTrash = (UIDFolder) trash;

            long numericUid = Long.parseLong(uid);
            Message message = uidTrash.getMessageByUID(numericUid);

            if (message == null) {
                throw new MailException("Email with UID " + uid + " no longer exists in Trash");
            }

            message.setFlag(Flags.Flag.DELETED, true);
            trash.expunge();

            log.info("Successfully deleted message {} permanently", uid);

        } catch (Exception e) {
            log.error("Failed to delete permanently: {}", e.getMessage(), e);
            throw new MailException("Failed to delete permanently: " + e.getMessage());
        } finally {
            try { if (trash != null && trash.isOpen()) trash.close(true); } catch (Exception e) {}
            try { if (store != null) store.close(); } catch (Exception e) {}
        }
    }

    public void markAsRead(String email, String password, String uid) {
        setSeenFlag(email, password, uid, true);
    }

    public void markAsUnread(String email, String password, String uid) {
        setSeenFlag(email, password, uid, false);
    }

    private void setSeenFlag(String email, String password, String uid, boolean seen) {
        jakarta.mail.Store store = null;
        jakarta.mail.Folder inbox = null;
        try {
            store = connect(email, password);

            inbox = store.getFolder("INBOX");
            inbox.open(jakarta.mail.Folder.READ_WRITE);

            if (!(inbox instanceof jakarta.mail.UIDFolder)) {
                throw new MailException("INBOX does not support persistent UIDs.");
            }
            jakarta.mail.UIDFolder uidInbox = (jakarta.mail.UIDFolder) inbox;

            long numericUid = Long.parseLong(uid);
            jakarta.mail.Message message = uidInbox.getMessageByUID(numericUid);

            if (message == null) {
                throw new MailException("Email with UID " + uid + " no longer exists in INBOX");
            }

            message.setFlag(jakarta.mail.Flags.Flag.SEEN, seen);
            log.info("Marked message {} as {} for {}", uid, seen ? "read" : "unread", email);
        } catch (Exception e) {
            log.error("Failed to set seen flag to {}: {}", seen, e.getMessage(), e);
            throw new MailException("Failed to update read status: " + e.getMessage());
        } finally {
            cleanup(store, inbox);
        }
    }

    public int getUnreadCount(String email, String password) {
        Store store = null;
        Folder inbox = null;
        try {
            store = connect(email, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            return inbox.getUnreadMessageCount();
        } catch (Exception e) {
            log.error("Failed to get unread count: {}", e.getMessage());
            return 0;
        } finally {
            cleanup(store, inbox);
        }
    }

    public EmailDTO getEmail(String email, String password, String uid) {
        log.info("Fetching single email details for UID: {}", uid);
        Store store = null;
        Folder inbox = null;
        try {
            store = connect(email, password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            if (!(inbox instanceof UIDFolder)) {
                throw new MailException("INBOX does not support persistent UIDs.");
            }
            UIDFolder uidInbox = (UIDFolder) inbox;

            long numericUid = Long.parseLong(uid);
            Message message = uidInbox.getMessageByUID(numericUid);

            if (message == null) {
                throw new MailException("Email with UID " + uid + " not found");
            }

            return convertToDTO(message, uidInbox, email);
        } catch (Exception e) {
            log.error("Failed to fetch email details: {}", e.getMessage());
            throw new MailException("Failed to fetch email: " + e.getMessage());
        } finally {
            cleanup(store, inbox);
        }
    }

    private String resolveTrashFolderName(Store store) throws MessagingException {
        String[] candidates = {"Trash", "TRASH", "Deleted Items", "Deleted", "INBOX.Trash", "INBOX/Trash"};
        for (String name : candidates) {
            try { 
                Folder f = store.getFolder(name);
                if (f.exists()) return name; 
            } catch (Exception e) {
                log.debug("Folder candidate '{}' check failed: {}", name, e.getMessage());
            }
        }
        try {
            Folder[] folders = store.getDefaultFolder().list("*");
            for (Folder f : folders) {
                String name = f.getFullName();
                if (name.equalsIgnoreCase("Trash") || name.toUpperCase().contains("TRASH") || name.toUpperCase().contains("DELETED")) return name;
            }
        } catch (MessagingException e) {
            log.warn("Failed to list all folders for trash resolution, falling back to 'Trash'");
        }
        return "Trash";
    }

    private String resolveSpamFolderName(Store store) throws MessagingException {
        String[] candidates = {"Spam", "SPAM", "Junk", "JUNK", "Junk Email", "INBOX.Spam", "INBOX.Junk", "[Gmail]/Spam"};
        for (String name : candidates) {
            try { 
                Folder f = store.getFolder(name);
                if (f.exists()) return name; 
            } catch (Exception e) {
                log.debug("Folder candidate '{}' check failed: {}", name, e.getMessage());
            }
        }
        try {
            Folder[] folders = store.getDefaultFolder().list("*");
            for (Folder f : folders) {
                String name = f.getFullName();
                if (name.equalsIgnoreCase("Spam") || name.toUpperCase().contains("SPAM") || name.toUpperCase().contains("JUNK")) return name;
            }
        } catch (MessagingException e) {
            log.warn("Failed to list all folders for spam resolution, falling back to 'Spam'");
        }
        return "Spam";
    }

    private String resolveSnoozedFolderName(Store store) throws MessagingException {
        String[] candidates = {"Snoozed", "SNOOZED", "INBOX.Snoozed", "INBOX/Snoozed", "Snooze"};
        for (String name : candidates) {
            try { 
                Folder f = store.getFolder(name);
                if (f.exists()) return name; 
            } catch (Exception e) {
                log.debug("Folder candidate '{}' check failed: {}", name, e.getMessage());
            }
        }
        
        // Scan for anything similar
        try {
            Folder[] folders = store.getDefaultFolder().list("*");
            for (Folder f : folders) {
                String name = f.getFullName();
                if (name.equalsIgnoreCase("Snoozed") || name.toUpperCase().contains("SNOOZE")) return name;
            }
        } catch (MessagingException e) {
            log.warn("Failed to scan folders for snoozed resolution");
        }

        // If not found, try to create it. Prefer INBOX prefix if candidates failed
        log.info("Snoozed folder not found, attempting to create 'Snoozed'");
        try {
            Folder f = store.getFolder("Snoozed");
            if (!f.exists()) f.create(Folder.HOLDS_MESSAGES);
            return "Snoozed";
        } catch (MessagingException e) {
            log.warn("Failed to create 'Snoozed' at root, trying 'INBOX.Snoozed'");
            Folder f = store.getFolder("INBOX.Snoozed");
            if (!f.exists()) f.create(Folder.HOLDS_MESSAGES);
            return "INBOX.Snoozed";
        }
    }



    public String resolveSentFolderName(Store store) throws MessagingException {
        // Broad list of shared folder names across various IMAP servers
        String[] candidates = {
            "Sent", "SENT", "Sent Messages", "Sent Items", 
            "INBOX.Sent", "INBOX/Sent", "[Gmail]/Sent Mail", "Sent Mail",
            "Sent Items", "SentMessages", "Outbox", "Sent Items", "Sent messages"
        };
        
        for (String name : candidates) {
            try { 
                Folder f = store.getFolder(name);
                if (f.exists()) {
                    log.debug("✓ Found Sent folder among candidates: {}", name);
                    return name;
                }
            } catch (Exception e) {
                log.trace("Folder candidate '{}' check failed: {}", name, e.getMessage());
            }
        }

        // Fallback: Scan ALL folders recursively
        try {
            log.info("Sent folder not in candidates, scanning ALL IMAP folders recursively...");
            Folder defaultFolder = store.getDefaultFolder();
            
            // Level 1 scan
            Folder[] folders = defaultFolder.list("*");
            for (Folder f : folders) {
                String name = f.getFullName();
                if (name.equalsIgnoreCase("Sent") || name.toUpperCase().contains("SENT")) {
                    log.info("✓ Found fallback Sent folder match via scan: {}", name);
                    return name;
                }
            }
            
            // Deep scan if needed
            for (Folder f : folders) {
                if ((f.getType() & Folder.HOLDS_FOLDERS) != 0) {
                    Folder[] sub = f.list("*");
                    for (Folder s : sub) {
                        String name = s.getFullName();
                        if (name.toUpperCase().contains("SENT")) {
                            log.info("✓ Found deep Sent folder match: {}", name);
                            return name;
                        }
                    }
                }
            }
            
            // Special check for INBOX prefix if not already found
            Folder inbox = store.getFolder("INBOX");
            if (inbox.exists()) {
                Folder[] subfolders = inbox.list("*");
                for (Folder f : subfolders) {
                    String name = f.getFullName();
                    if (name.toUpperCase().contains("SENT")) {
                        log.info("✓ Found match via INBOX scan: {}", name);
                        return name;
                    }
                }
            }
        } catch (MessagingException e) {
             log.warn("Failed to list all folders for sent resolution: {}", e.getMessage());
        }

        log.warn("⚠ NO SENT FOLDER DETECTED for user. Using default 'Sent'.");
        return "Sent";
    }


    private EmailDTO convertToDTO(Message message, UIDFolder uidFolder, String userEmail) throws MessagingException, IOException {
        EmailDTO dto = new EmailDTO();
        
        String uidStr;
        // Use persistent UID if available, fallback to message number
        if (uidFolder != null) {
            uidStr = String.valueOf(uidFolder.getUID(message));
        } else {
            uidStr = String.valueOf(message.getMessageNumber());
        }
        dto.setUid(uidStr);

        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            Address addr = from[0];
            if (addr instanceof InternetAddress) {
                dto.setFrom(addr.toString());
            } else {
                dto.setFrom(addr.toString());
            }
        }

        Address[] to = message.getRecipients(Message.RecipientType.TO);
        if (to != null && to.length > 0) {
            Address addr = to[0];
            if (addr instanceof InternetAddress) {
                dto.setTo(addr.toString());
            } else {
                dto.setTo(addr.toString());
            }
        }
        
        dto.setSubject(message.getSubject());
        dto.setSentDate(message.getSentDate());
        dto.setReceivedDate(message.getReceivedDate());
        dto.setRead(message.isSet(Flags.Flag.SEEN));
        
        // Use Database mapping for star status
        String actualFolderName = "INBOX";
        try {
            Folder f = message.getFolder();
            if (f != null) {
                actualFolderName = f.getFullName();
            }
        } catch (Exception e) {
            log.debug("Could not determine folder for message: {}", e.getMessage());
        }
        
        dto.setStarred(starredEmailRepository.existsByUserEmailAndUidAndFolderName(userEmail, uidStr, actualFolderName));
        dto.setSize(message.getSize());
        
        String[] content = extractContent(message);
        dto.setBody(content[0]);
        dto.setHtmlBody(content[1]);
        dto.setHasAttachments(hasAttachments(message));
        if (dto.isHasAttachments()) dto.setAttachments(extractAttachments(message));
        
        String category = categorizeEmail(message);
        
        // Override category based on actual folder location
        String upperFolder = actualFolderName.toUpperCase();
        if (upperFolder.contains("SENT")) category = "SENT";
        else if (upperFolder.contains("TRASH") || upperFolder.contains("DELETED")) category = "TRASH";
        else if (upperFolder.contains("SPAM") || upperFolder.contains("JUNK")) category = "SPAM";
        
        dto.setCategory(category);
        // Labels (V3)
        dto.setLabels(labelMappingRepository.findByUserEmailAndEmailUidAndFolderName(userEmail, dto.getUid(), actualFolderName)
                .stream()
                .map(MailLabelMapping::getLabel)
                .collect(java.util.stream.Collectors.toList()));

        return dto;
    }




    private String categorizeEmail(Message message) {
        if (message == null) return "PRIMARY";
        try {
            String subject = "";
            try { 
                subject = message.getSubject() != null ? message.getSubject().toUpperCase() : "";
            } catch (Exception e) {
                log.debug("Could not read subject for categorization: {}", e.getMessage());
            }

            String from = "";
            try {
                Address[] fromAddresses = message.getFrom();
                if (fromAddresses != null && fromAddresses.length > 0) {
                    from = fromAddresses[0].toString().toLowerCase();
                }
            } catch (Exception e) {
                log.debug("Could not read sender for categorization: {}", e.getMessage());
            }

            // 0. Spam Detection (Highest Priority)
            if (subject.contains("LOTTERY") || subject.contains("WINNER") || 
                subject.contains("INHERITANCE") || subject.contains("VIAGRA") || 
                subject.contains("CASINO") || subject.contains("JACKPOT") ||
                subject.contains("URGENT ACTION REQUIRED") || subject.contains("ACCOUNT SUSPENDED")) {
                return "SPAM";
            }

            // 1. Social
            if (from.contains("facebook.com") || from.contains("instagram.com") || 
                from.contains("linkedin.com") || from.contains("twitter.com") || 
                from.contains("t.co") || from.contains("fb.com") || from.contains("social")) {
                return "SOCIAL";
            }

            // 2. Purchases
            if (subject.contains("ORDER") || subject.contains("INVOICE") || 
                subject.contains("RECEIPT") || subject.contains("PURCHASE") || 
                subject.contains("AMAZON") || subject.contains("FLIPKART") ||
                subject.contains("BILL") || subject.contains("PAYMENT")) {
                return "PURCHASES";
            }

            // 3. Updates (System/Alerts/Bounces)
            if (subject.contains("OTP") || subject.contains("VERIFICATION") || 
                subject.contains("PASSWORD") || subject.contains("SHIPPED") || 
                subject.contains("ALERT") || subject.contains("SECURITY") ||
                subject.contains("ACCOUNT") || subject.contains("LOGIN") ||
                subject.contains("UNDELIVERED") || subject.contains("RETURNED TO SENDER") ||
                from.contains("mailer-daemon") || from.contains("postmaster")) {
                return "UPDATES";
            }

            // 4. Promotions (Marketing)
            String[] unsubscribe = message.getHeader("List-Unsubscribe");
            if ((unsubscribe != null && unsubscribe.length > 0) || 
                subject.contains("SALE") || subject.contains("OFFER") || 
                subject.contains("DISCOUNT") || subject.contains("DEAL") || 
                subject.contains("PROMOTION") || subject.contains("LIMITED TIME") ||
                subject.contains("% OFF")) {
                return "PROMOTIONS";
            }

            // 5. Default: Primary
            return "PRIMARY";

        } catch (Exception e) {
            log.warn("Failed to categorize email: {}", e.getMessage());
            return "PRIMARY";
        }
    }

    private String[] extractContent(Message message) throws MessagingException, IOException {
        String plain = ""; String html = "";
        Object content = message.getContent();
        if (content instanceof String) plain = (String) content;
        else if (content instanceof Multipart) {
            Multipart mp = (Multipart) content;
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) plain = (String) bp.getContent();
                else if (bp.isMimeType("text/html")) html = (String) bp.getContent();
                else if (bp.getContent() instanceof MimeMultipart) {
                    MimeMultipart nested = (MimeMultipart) bp.getContent();
                    for (int j = 0; j < nested.getCount(); j++) {
                        BodyPart nb = nested.getBodyPart(j);
                        if (nb.isMimeType("text/plain")) plain = (String) nb.getContent();
                        else if (nb.isMimeType("text/html")) html = (String) nb.getContent();
                    }
                }
            }
        }
        if (plain.isEmpty() && !html.isEmpty()) plain = html.replaceAll("<[^>]*>", "").trim();
        return new String[]{plain, html};
    }

    private boolean hasAttachments(Message message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof Multipart) {
            Multipart mp = (Multipart) content;
            for (int i = 0; i < mp.getCount(); i++) {
                if (Part.ATTACHMENT.equalsIgnoreCase(mp.getBodyPart(i).getDisposition())) return true;
            }
        }
        return false;
    }

    private List<String> extractAttachments(Part part) throws MessagingException, IOException {
        List<String> names = new ArrayList<>();
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) names.addAll(extractAttachments(mp.getBodyPart(i)));
        } else {
            String disp = part.getDisposition();
            if (Part.ATTACHMENT.equalsIgnoreCase(disp) || Part.INLINE.equalsIgnoreCase(disp)) {
                String name = part.getFileName();
                if (name != null) names.add(jakarta.mail.internet.MimeUtility.decodeText(name));
            }
        }
        return names;
    }

    public Store connect(String email, String password) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", String.valueOf(imapPort));
        props.put("mail.imap.ssl.enable", String.valueOf(imapSslEnable));
        props.put("mail.debug", "true"); // CRITICAL: Enable debug logs to see IMAP traffic
        if (imapSslEnable) {
            props.put("mail.imap.ssl.trust", "*");
        }
        // Set timeouts to prevent hanging threads
        props.put("mail.imap.connectiontimeout", "10000");
        props.put("mail.imap.timeout", "10000");

        Session session = Session.getInstance(props);
        Store store = session.getStore(imapProtocol);
        store.connect(imapHost, imapPort, email, password);
        return store;
    }

    private void cleanup(Store store, Folder folder) {
        try { if (folder != null && folder.isOpen()) folder.close(false); } catch (Exception e) {}
        try { if (store != null) store.close(); } catch (Exception e) {}
    }

    public void downloadAttachment(String email, String password, String uid, String fileName, java.io.OutputStream os) {
        Store store = null; Folder folder = null;
        try {
            store = connect(email, password);
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            if (!(folder instanceof UIDFolder)) {
                 throw new MailException("INBOX does not support persistent UIDs.");
            }
            UIDFolder uidFolder = (UIDFolder) folder;

            Message message = uidFolder.getMessageByUID(Long.parseLong(uid));
            if (message == null) throw new MailException("Email with UID " + uid + " not found");

            if (message.getContent() instanceof Multipart) findAndWriteAttachment((Multipart) message.getContent(), fileName, os);
            else throw new MailException("No attachments found");
        } catch (Exception e) {
            log.error("Download failed for UID {}: {}", uid, e.getMessage());
            throw new MailException("Download failed: " + e.getMessage());
        } finally {
            cleanup(store, folder);
        }
    }

    private void findAndWriteAttachment(Multipart mp, String name, java.io.OutputStream os) throws MessagingException, IOException {
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart bp = mp.getBodyPart(i);
            String fn = bp.getFileName();
            if (fn != null && jakarta.mail.internet.MimeUtility.decodeText(fn).equalsIgnoreCase(name)) {
                bp.getDataHandler().getInputStream().transferTo(os);
                return;
            }
            if (bp.isMimeType("multipart/*")) findAndWriteAttachment((Multipart) bp.getContent(), name, os);
        }
    }
}
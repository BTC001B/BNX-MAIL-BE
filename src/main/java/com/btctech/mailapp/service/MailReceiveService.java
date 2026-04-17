package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.EmailDTO;
import com.btctech.mailapp.exception.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
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
            }

            folder = store.getFolder(actualFolderName);
            if (!folder.exists()) {
                log.warn("Folder '{}' does not exist for user {}", actualFolderName, email);
                return new ArrayList<>();
            }
            
            folder.open(Folder.READ_ONLY);

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
                    emails.add(convertToDTO(messages[i], uidFolder));
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

    public List<EmailDTO> getStarred(String email, String password, int limit) {
        log.info("Fetching starred messages for: {}", email);
        Store store = null;
        Folder folder = null;

        try {
            store = connect(email, password);

            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            if (!(folder instanceof UIDFolder)) {
                throw new MailException("INBOX does not support UIDs, required for Starred feature.");
            }
            UIDFolder uidFolder = (UIDFolder) folder;

            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.FLAGGED), true));
            List<EmailDTO> emails = new ArrayList<>();
            int count = 0;
            for (int i = messages.length - 1; i >= 0 && count < limit; i--) {
                try {
                    emails.add(convertToDTO(messages[i], uidFolder));
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to parse starred message: {}", e.getMessage());
                }
            }
            return emails;
        } catch (MessagingException e) {
            log.error("Failed to fetch starred emails: {}", e.getMessage(), e);
            throw new MailException("Failed to fetch starred emails: " + e.getMessage());
        } finally {
            cleanup(store, folder);
        }
    }

    public void toggleStar(String email, String password, String folderName, String uid) {
        log.info("Toggling star for email UID {} in folder {}", uid, folderName);
        Store store = null;
        Folder folder = null;

        try {
            store = connect(email, password);

            String actualFolderName = folderName;
            if ("Sent".equalsIgnoreCase(folderName)) actualFolderName = resolveSentFolderName(store);
            else if ("Trash".equalsIgnoreCase(folderName)) actualFolderName = resolveTrashFolderName(store);

            folder = store.getFolder(actualFolderName);
            folder.open(Folder.READ_WRITE);

            if (!(folder instanceof UIDFolder)) {
                throw new MailException("Folder " + actualFolderName + " does not support persistent UIDs.");
            }
            UIDFolder uidFolder = (UIDFolder) folder;
            
            long numericUid = Long.parseLong(uid);
            Message message = uidFolder.getMessageByUID(numericUid);
            
            if (message == null) {
                throw new MailException("Email with UID " + uid + " no longer exists in " + actualFolderName);
            }
            
            boolean currentStatus = message.isSet(Flags.Flag.FLAGGED);
            message.setFlag(Flags.Flag.FLAGGED, !currentStatus);
            
            log.info("Successfully toggled star for UID {}. New status: {}", uid, !currentStatus);

        } catch (Exception e) {
            log.error("Failed to toggle star for UID {}: {}", uid, e.getMessage(), e);
            throw new MailException("Failed to toggle star: " + e.getMessage());
        } finally {
            cleanup(store, folder);
        }
    }

    public void moveToTrash(String email, String password, String sourceFolderName, String uid) {
        log.info("Moving email UID {} from {} to Trash for {}", uid, sourceFolderName, email);
        Store store = null;
        Folder source = null;
        Folder trash = null;

        try {
            store = connect(email, password);

            String trashFolderName = resolveTrashFolderName(store);
            trash = store.getFolder(trashFolderName);
            if (!trash.exists()) trash.create(Folder.HOLDS_MESSAGES);

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

            source.copyMessages(new Message[]{message}, trash);
            message.setFlag(Flags.Flag.DELETED, true);
            source.expunge();

            log.info("Successfully moved message {} to {}", uid, trashFolderName);

        } catch (Exception e) {
            log.error("Failed to move to trash for UID {}: {}", uid, e.getMessage(), e);
            throw new MailException("Failed to move to trash: " + e.getMessage());
        } finally {
            try { if (source != null && source.isOpen()) source.close(true); } catch (Exception e) {}
            try { if (store != null) store.close(); } catch (Exception e) {}
        }
    }

    public void restoreFromTrash(String email, String password, String uid) {
        log.info("Restoring email UID {} from Trash to Inbox for {}", uid, email);
        Store store = null;
        Folder trash = null;
        Folder inbox = null;

        try {
            store = connect(email, password);

            String trashName = resolveTrashFolderName(store);
            trash = store.getFolder(trashName);
            trash.open(Folder.READ_WRITE);

            if (!(trash instanceof UIDFolder)) {
                throw new MailException("Trash folder does not support persistent UIDs.");
            }
            UIDFolder uidTrash = (UIDFolder) trash;

            inbox = store.getFolder("INBOX");
            long numericUid = Long.parseLong(uid);
            Message message = uidTrash.getMessageByUID(numericUid);

            if (message == null) {
                throw new MailException("Email with UID " + uid + " no longer exists in Trash");
            }

            trash.copyMessages(new Message[]{message}, inbox);
            message.setFlag(Flags.Flag.DELETED, true);
            trash.expunge();

            log.info("Successfully restored message {} to Inbox", uid);

        } catch (Exception e) {
            log.error("Failed to restore from trash: {}", e.getMessage(), e);
            throw new MailException("Failed to restore from trash: " + e.getMessage());
        } finally {
            try { if (trash != null && trash.isOpen()) trash.close(true); } catch (Exception e) {}
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
        Store store = null;
        Folder inbox = null;
        try {
            store = connect(email, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            if (!(inbox instanceof UIDFolder)) {
                throw new MailException("INBOX does not support persistent UIDs.");
            }
            UIDFolder uidInbox = (UIDFolder) inbox;

            long numericUid = Long.parseLong(uid);
            Message message = uidInbox.getMessageByUID(numericUid);

            if (message == null) {
                throw new MailException("Email with UID " + uid + " no longer exists in INBOX");
            }

            message.setFlag(Flags.Flag.SEEN, true);
            log.info("Marked message {} as read for {}", uid, email);
        } catch (Exception e) {
            log.error("Failed to mark as read: {}", e.getMessage(), e);
            throw new MailException("Failed to mark as read: " + e.getMessage());
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

            return convertToDTO(message, uidInbox);
        } catch (Exception e) {
            log.error("Failed to fetch email details: {}", e.getMessage());
            throw new MailException("Failed to fetch email: " + e.getMessage());
        } finally {
            cleanup(store, inbox);
        }
    }

    private String resolveTrashFolderName(Store store) throws MessagingException {
        String[] candidates = {"Trash", "TRASH", "Deleted Items", "Deleted", "INBOX.Trash"};
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
                String name = f.getName();
                if (name.equalsIgnoreCase("Trash") || name.toUpperCase().contains("TRASH") || name.toUpperCase().contains("DELETED")) return name;
            }
        } catch (MessagingException e) {
            log.warn("Failed to list all folders for trash resolution, falling back to 'Trash'");
        }
        return "Trash";
    }

    private String resolveSentFolderName(Store store) throws MessagingException {
        String[] candidates = {"Sent", "SENT", "Sent Messages", "Sent Items", "INBOX.Sent"};
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
                String name = f.getName();
                if (name.equalsIgnoreCase("Sent") || name.toUpperCase().contains("SENT")) return name;
            }
        } catch (MessagingException e) {
             log.warn("Failed to list all folders for sent resolution, falling back to 'Sent'");
        }
        return "Sent";
    }

    private EmailDTO convertToDTO(Message message, UIDFolder uidFolder) throws MessagingException, IOException {
        EmailDTO dto = new EmailDTO();
        
        // Use persistent UID if available, fallback to message number
        if (uidFolder != null) {
            dto.setUid(String.valueOf(uidFolder.getUID(message)));
        } else {
            dto.setUid(String.valueOf(message.getMessageNumber()));
        }

        Address[] from = message.getFrom();
        if (from != null && from.length > 0) dto.setFrom(((InternetAddress) from[0]).getAddress());
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        if (to != null && to.length > 0) dto.setTo(((InternetAddress) to[0]).getAddress());
        dto.setSubject(message.getSubject());
        dto.setSentDate(message.getSentDate());
        dto.setReceivedDate(message.getReceivedDate());
        dto.setRead(message.isSet(Flags.Flag.SEEN));
        dto.setStarred(message.isSet(Flags.Flag.FLAGGED));
        dto.setSize(message.getSize());
        String[] content = extractContent(message);
        dto.setBody(content[0]);
        dto.setHtmlBody(content[1]);
        dto.setHasAttachments(hasAttachments(message));
        if (dto.isHasAttachments()) dto.setAttachments(extractAttachments(message));
        return dto;
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

    private Store connect(String email, String password) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", String.valueOf(imapPort));
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.trust", "*");
        // Set timeouts to prevent hanging threads
        props.put("mail.imap.connectiontimeout", "10000");
        props.put("mail.imap.timeout", "10000");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imap");
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
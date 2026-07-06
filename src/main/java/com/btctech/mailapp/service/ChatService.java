package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.Chat;
import com.btctech.mailapp.entity.ChatMessage;
import com.btctech.mailapp.entity.ChatType;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.repository.ChatMessageRepository;
import com.btctech.mailapp.repository.ChatRepository;
import com.btctech.mailapp.repository.UserRepository;
import com.btctech.mailapp.entity.ChatInvitation;
import com.btctech.mailapp.entity.InvitationStatus;
import com.btctech.mailapp.entity.GroupBroadcast;
import com.btctech.mailapp.repository.ChatInvitationRepository;
import com.btctech.mailapp.repository.GroupBroadcastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final GroupBroadcastRepository groupBroadcastRepository;
    private final ChatInvitationRepository chatInvitationRepository;

    @Transactional
    public Chat createDirectChat(String email1, String email2) {
        User user1 = userRepository.findByEmail(email1)
                .orElseThrow(() -> new RuntimeException("User not found: " + email1));
        User user2 = userRepository.findByEmail(email2)
                .orElseThrow(() -> new RuntimeException("User not found: " + email2));

        Optional<Chat> existing = chatRepository.findDirectChatBetweenUsers(user1, user2);
        if (existing.isPresent()) {
            return existing.get();
        }

        Chat chat = new Chat();
        chat.setType(ChatType.DIRECT);
        chat.getMembers().add(user1);
        chat.getMembers().add(user2);
        return chatRepository.save(chat);
    }

    @Transactional
    public Chat createGroupChat(String name, List<String> memberEmails, String creatorEmail) {
        Chat chat = new Chat();
        chat.setName(name);
        chat.setType(ChatType.GROUP);
        
        Set<User> members = new HashSet<>();
        User creator = userRepository.findByEmail(creatorEmail).orElseGet(() -> userRepository.findByUsername(creatorEmail).orElse(null));
        if (creator != null) {
            members.add(creator);
        }
        chat.setMembers(members);
        Chat savedChat = chatRepository.save(chat);

        for (String email : memberEmails) {
            if (email.equals(creatorEmail)) continue;
            userRepository.findByEmail(email).ifPresent(invitee -> {
                Optional<ChatInvitation> existing = chatInvitationRepository.findByChatAndInviteeAndStatus(savedChat, invitee, InvitationStatus.PENDING);
                if (existing.isEmpty()) {
                    ChatInvitation invitation = new ChatInvitation();
                    invitation.setChat(savedChat);
                    invitation.setInviter(creator);
                    invitation.setInvitee(invitee);
                    invitation.setStatus(InvitationStatus.PENDING);
                    chatInvitationRepository.save(invitation);
                }
            });
        }

        return savedChat;
    }

    public List<Chat> getUserChats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return chatRepository.findByMembersContaining(user);
    }

    public List<ChatMessage> getChatMessages(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        return chatMessageRepository.findByChatOrderByTimestampAsc(chat);
    }

    @Transactional
    public ChatMessage saveMessage(Long chatId, String senderEmail, String content, String attachmentsJson) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderEmail(senderEmail);
        message.setContent(content);
        message.setAttachmentsJson(attachmentsJson);
        return chatMessageRepository.save(message);
    }

    @Transactional
    public Chat inviteMembersToGroup(Long chatId, List<String> emails, String inviterEmail) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        if (chat.getType() != ChatType.GROUP) {
            throw new RuntimeException("Cannot add members to a non-group chat");
        }
        User inviter = userRepository.findByEmail(inviterEmail).orElseGet(() -> userRepository.findByUsername(inviterEmail).orElse(null));

        for (String email : emails) {
            userRepository.findByEmail(email).ifPresent(invitee -> {
                // Check if already a member
                if (chat.getMembers().contains(invitee)) return;

                Optional<ChatInvitation> existing = chatInvitationRepository.findByChatAndInviteeAndStatus(chat, invitee, InvitationStatus.PENDING);
                if (existing.isEmpty()) {
                    ChatInvitation invitation = new ChatInvitation();
                    invitation.setChat(chat);
                    invitation.setInviter(inviter);
                    invitation.setInvitee(invitee);
                    invitation.setStatus(InvitationStatus.PENDING);
                    chatInvitationRepository.save(invitation);
                }
            });
        }
        return chat;
    }

    public List<ChatInvitation> getPendingInvitations(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseGet(() -> userRepository.findByUsername(userEmail).orElse(null));
        if (user == null) throw new RuntimeException("User not found: " + userEmail);
        return chatInvitationRepository.findByInviteeAndStatus(user, InvitationStatus.PENDING);
    }

    @Transactional
    public void acceptInvitation(Long invitationId, String userEmail) {
        ChatInvitation invitation = chatInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        
        User invitee = invitation.getInvitee();
        if (!invitee.getEmail().equals(userEmail) && !invitee.getUsername().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Invitation is not pending");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        chatInvitationRepository.save(invitation);

        Chat chat = invitation.getChat();
        chat.getMembers().add(invitation.getInvitee());
        chatRepository.save(chat);
    }

    @Transactional
    public void rejectInvitation(Long invitationId, String userEmail) {
        ChatInvitation invitation = chatInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        
        User invitee = invitation.getInvitee();
        if (!invitee.getEmail().equals(userEmail) && !invitee.getUsername().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Invitation is not pending");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        chatInvitationRepository.save(invitation);
    }

    public List<User> getChatMembers(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        return List.copyOf(chat.getMembers());
    }

    @Transactional
    public GroupBroadcast saveBroadcast(Long chatId, String senderEmail, String subject, String body, String attachmentsJson) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        if (chat.getType() != ChatType.GROUP) {
            throw new RuntimeException("Cannot send broadcast to a non-group chat");
        }
        GroupBroadcast broadcast = GroupBroadcast.builder()
                .chat(chat)
                .senderEmail(senderEmail)
                .subject(subject)
                .body(body)
                .attachmentsJson(attachmentsJson)
                .sentDate(java.time.LocalDateTime.now())
                .build();
        return groupBroadcastRepository.save(broadcast);
    }

    public List<GroupBroadcast> getBroadcasts(Long chatId) {
        return groupBroadcastRepository.findByChatIdOrderBySentDateDesc(chatId);
    }
}

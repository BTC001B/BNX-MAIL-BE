package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.Chat;
import com.btctech.mailapp.entity.ChatMessage;
import com.btctech.mailapp.entity.ChatType;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.repository.ChatMessageRepository;
import com.btctech.mailapp.repository.ChatRepository;
import com.btctech.mailapp.repository.UserRepository;
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
    public Chat createGroupChat(String name, List<String> memberEmails) {
        Chat chat = new Chat();
        chat.setName(name);
        chat.setType(ChatType.GROUP);
        
        Set<User> members = new HashSet<>();
        for (String email : memberEmails) {
            userRepository.findByEmail(email).ifPresent(members::add);
        }
        chat.setMembers(members);
        return chatRepository.save(chat);
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
    public ChatMessage saveMessage(Long chatId, String senderEmail, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderEmail(senderEmail);
        message.setContent(content);
        return chatMessageRepository.save(message);
    }
}

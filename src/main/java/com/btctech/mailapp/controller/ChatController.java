package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ChatDTO;
import com.btctech.mailapp.entity.Chat;
import com.btctech.mailapp.entity.ChatMessage;
import com.btctech.mailapp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final com.btctech.mailapp.repository.ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/direct")
    public ResponseEntity<ChatDTO> createDirectChat(@RequestBody ChatDTO.CreateDirect request) {
        Chat chat = chatService.createDirectChat(request.getUser1(), request.getUser2());
        return ResponseEntity.ok(convertToDTO(chat));
    }

    @PostMapping("/group")
    public ResponseEntity<ChatDTO> createGroupChat(@RequestBody ChatDTO.CreateGroup request) {
        Chat chat = chatService.createGroupChat(request.getName(), request.getMembers());
        return ResponseEntity.ok(convertToDTO(chat));
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<List<ChatDTO>> getUserChats(@PathVariable String email) {
        List<Chat> chats = chatService.getUserChats(email);
        return ResponseEntity.ok(chats.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatDTO.MessageResponse>> getChatMessages(@PathVariable Long chatId) {
        List<ChatMessage> messages = chatService.getChatMessages(chatId);
        return ResponseEntity.ok(messages.stream().map(this::convertToMessageResponse).collect(Collectors.toList()));
    }

    @PostMapping("/message")
    public ResponseEntity<ChatDTO.MessageResponse> sendMessageRest(@RequestBody ChatDTO.MessageRequest request) {
        ChatMessage saved = chatService.saveMessage(request.getChatId(), request.getSender(), request.getMessage());
        ChatDTO.MessageResponse response = convertToMessageResponse(saved);
        
        // Even if sent via REST, we broadcast to WebSocket for real-time
        messagingTemplate.convertAndSend("/topic/chat/" + request.getChatId(), response);
        
        return ResponseEntity.ok(response);
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatDTO.MessageRequest request) {
        ChatMessage saved = chatService.saveMessage(request.getChatId(), request.getSender(), request.getMessage());
        ChatDTO.MessageResponse response = convertToMessageResponse(saved);
        
        // Broadcast to the chat topic
        messagingTemplate.convertAndSend("/topic/chat/" + request.getChatId(), response);
    }

    private ChatDTO convertToDTO(Chat chat) {
        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setName(chat.getName());
        dto.setType(chat.getType());
        dto.setMemberEmails(chat.getMembers().stream()
                .map(u -> u.getEmail() != null ? u.getEmail() : u.getUsername())
                .collect(Collectors.toList()));
        
        // Populate last message info efficiently
        chatMessageRepository.findFirstByChatOrderByTimestampDesc(chat).ifPresentOrElse(last -> {
            dto.setLastMessage(last.getContent());
            dto.setLastMessageTime(last.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }, () -> {
            dto.setLastMessage("");
            dto.setLastMessageTime("");
        });
        dto.setUnreadCount(0);
        
        return dto;
    }

    private ChatDTO.MessageResponse convertToMessageResponse(ChatMessage message) {
        ChatDTO.MessageResponse response = new ChatDTO.MessageResponse();
        response.setId(message.getId());
        response.setChatId(message.getChat().getId());
        response.setSender(message.getSenderEmail());
        response.setContent(message.getContent());
        response.setTimestamp(message.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }
}

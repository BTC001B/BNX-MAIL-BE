package com.btctech.mailapp.dto;

import com.btctech.mailapp.entity.ChatType;
import lombok.Data;
import java.util.List;

@Data
public class ChatDTO {
    private Long id;
    private String name;
    private ChatType type;
    private List<String> memberEmails;
    private String lastMessage;
    private String lastMessageTime;
    private Integer unreadCount;

    @Data
    public static class CreateDirect {
        private String user1;
        private String user2;
    }

    @Data
    public static class CreateGroup {
        private String name;
        private List<String> members;
    }

    @Data
    public static class MessageRequest {
        private Long chatId;
        private String sender;
        private String message;
    }

    @Data
    public static class MessageResponse {
        private Long id;
        private Long chatId;
        private String sender;
        private String content;
        private String timestamp;
    }
}

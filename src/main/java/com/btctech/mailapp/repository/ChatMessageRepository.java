package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.Chat;
import com.btctech.mailapp.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatOrderByTimestampAsc(Chat chat);
    
    Optional<ChatMessage> findFirstByChatOrderByTimestampDesc(Chat chat);
}

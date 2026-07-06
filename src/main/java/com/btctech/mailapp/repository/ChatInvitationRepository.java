package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.ChatInvitation;
import com.btctech.mailapp.entity.InvitationStatus;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatInvitationRepository extends JpaRepository<ChatInvitation, Long> {
    List<ChatInvitation> findByInviteeAndStatus(User invitee, InvitationStatus status);
    Optional<ChatInvitation> findByChatAndInviteeAndStatus(Chat chat, User invitee, InvitationStatus status);
    
    void deleteByChat(Chat chat);
}

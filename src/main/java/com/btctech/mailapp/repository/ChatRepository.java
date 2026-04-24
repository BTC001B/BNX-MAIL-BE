package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.Chat;
import com.btctech.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    @Query("SELECT c FROM Chat c LEFT JOIN FETCH c.members WHERE :user MEMBER OF c.members")
    List<Chat> findByMembersContaining(@Param("user") User user);

    @Query("SELECT c FROM Chat c JOIN FETCH c.members m1 JOIN FETCH c.members m2 WHERE c.type = 'DIRECT' AND m1 = :user1 AND m2 = :user2")
    Optional<Chat> findDirectChatBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
}

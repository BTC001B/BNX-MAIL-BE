package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.GroupBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupBroadcastRepository extends JpaRepository<GroupBroadcast, Long> {
    List<GroupBroadcast> findByChatIdOrderBySentDateDesc(Long chatId);
}

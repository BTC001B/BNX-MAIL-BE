package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserIdOrderByTimestampDesc(Long userId);
    List<ActivityLog> findTop20ByUserIdOrderByTimestampDesc(Long userId);
    List<ActivityLog> findTop10ByOrderByTimestampDesc();
    
    org.springframework.data.domain.Page<ActivityLog> findAllByOrderByTimestampDesc(org.springframework.data.domain.Pageable pageable);
    
    @org.springframework.data.jpa.repository.Query("SELECT a FROM ActivityLog a WHERE " +
           "LOWER(a.activity) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.details) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.user.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.user.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY a.timestamp DESC")
    org.springframework.data.domain.Page<ActivityLog> searchLogs(String query, org.springframework.data.domain.Pageable pageable);
}

package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByReportedUserId(Long reportedUserId);
    List<Report> findByReportedUserIdOrderByCreatedAtDesc(Long reportedUserId);
    void deleteByReportedUserId(Long reportedUserId);
}

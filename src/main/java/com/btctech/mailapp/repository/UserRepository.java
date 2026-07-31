package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"organization"})
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // Find user by email (joining with MailAccount)
    @Query("SELECT u FROM User u JOIN MailAccount m ON u.id = m.userId WHERE m.email = :email")
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);
}

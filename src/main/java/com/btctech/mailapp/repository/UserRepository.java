package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // Find user by email (joining with MailAccount)
    @Query("SELECT u FROM User u JOIN MailAccount m ON u.id = m.userId WHERE m.email = :email")
    Optional<User> findByEmail(String email);
}

package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_settings")
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Personal Info Extensions
    private String phoneNumber;
    private String location;
    private String jobTitle;

    // Notification Settings
    @Builder.Default
    private Boolean inboxNotifications = true;
    @Builder.Default
    private Boolean sentNotifications = false;
    @Builder.Default
    private Boolean starredNotifications = true;
    @Builder.Default
    private Boolean snoozedNotifications = true;
    @Builder.Default
    private Boolean soundEnabled = true;
    @Builder.Default
    private Boolean vibrationEnabled = true;
    @Builder.Default
    private Boolean quietHoursEnabled = false;
    @Builder.Default
    private String quietHoursStart = "22:00";
    @Builder.Default
    private String quietHoursEnd = "07:00";

    // Appearance Settings
    @Builder.Default
    private String themeMode = "System Default"; // Light, Dark, System Default
    @Builder.Default
    private String accentColor = "#4F46E5"; // Default primary color
    @Builder.Default
    private Double fontSize = 1.0;
    @Builder.Default
    private String density = "Default"; // Compact, Default, Comfortable

    @Builder.Default
    private Long storageLimit = 16106127360L; // 15 GB in bytes

    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Builder.Default
    private Boolean biometricsEnabled = true;

    @Builder.Default
    private String language = "en_US";
}

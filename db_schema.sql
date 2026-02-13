-- Create mail application database
CREATE DATABASE IF NOT EXISTS mail_app_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mail_app_db;

-- ==========================================
-- TABLE 1: USERS (Application users with JWT auth)
-- ==========================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    role VARCHAR(20) DEFAULT 'USER',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- ==========================================
-- TABLE 2: MAIL_ACCOUNTS (Link users to VPS mail accounts)
-- ==========================================
CREATE TABLE IF NOT EXISTS mail_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    vps_username VARCHAR(50) NOT NULL,
    vps_password VARCHAR(255) NOT NULL,
    smtp_host VARCHAR(100) DEFAULT 'mail.btctech.shop',
    smtp_port INT DEFAULT 587,
    imap_host VARCHAR(100) DEFAULT 'mail.btctech.shop',
    imap_port INT DEFAULT 993,
    is_default BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_email (email_address)
) ENGINE=InnoDB;

-- ==========================================
-- TABLE 3: FOLDERS (Mail folders)
-- ==========================================
CREATE TABLE IF NOT EXISTS folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_account_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    folder_type VARCHAR(20) DEFAULT 'CUSTOM',
    parent_id BIGINT NULL,
    unread_count INT DEFAULT 0,
    total_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mail_account_id) REFERENCES mail_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE CASCADE,
    INDEX idx_account (mail_account_id)
) ENGINE=InnoDB;

-- ==========================================
-- TABLE 4: MAILS (Email metadata for fast access)
-- ==========================================
CREATE TABLE IF NOT EXISTS mails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_account_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    message_id VARCHAR(255),
    uid VARCHAR(100),
    from_address VARCHAR(255),
    to_address TEXT,
    cc_address TEXT,
    bcc_address TEXT,
    subject VARCHAR(500),
    body_text LONGTEXT,
    body_html LONGTEXT,
    sent_date DATETIME,
    received_date DATETIME,
    is_read BOOLEAN DEFAULT FALSE,
    is_starred BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    has_attachments BOOLEAN DEFAULT FALSE,
    size_bytes BIGINT DEFAULT 0,
    in_reply_to VARCHAR(255),
    `references` TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mail_account_id) REFERENCES mail_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_folder (folder_id),
    INDEX idx_sent_date (sent_date),
    INDEX idx_is_read (is_read),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB;

-- ==========================================
-- TABLE 5: ATTACHMENTS
-- ==========================================
CREATE TABLE IF NOT EXISTS attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    file_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mail_id) REFERENCES mails(id) ON DELETE CASCADE,
    INDEX idx_mail_id (mail_id)
) ENGINE=InnoDB;

-- ==========================================
-- TABLE 6: CONTACTS
-- ==========================================
CREATE TABLE IF NOT EXISTS contacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(50),
    company VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- ==========================================
-- TABLE 7: LABELS/TAGS
-- ==========================================
CREATE TABLE IF NOT EXISTS labels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) DEFAULT '#000000',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_label (user_id, name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mail_labels (
    mail_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    PRIMARY KEY (mail_id, label_id),
    FOREIGN KEY (mail_id) REFERENCES mails(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES labels(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================
-- SAMPLE DATA
-- ==========================================
-- Using IGNORE to avoid errors if duplicates exist
INSERT IGNORE INTO users (username, email, password, first_name, last_name, role) 
VALUES 
('admin', 'admin@btctech.shop', '$2a$10$dummyHashedPassword', 'Admin', 'User', 'ADMIN'),
('siva', 'siva@btctech.shop', '$2a$10$dummyHashedPassword', 'Siva', 'Kumar', 'USER');

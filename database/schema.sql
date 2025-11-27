-- =============================================
-- Chat Application Database Schema
-- MySQL Database
-- =============================================

CREATE DATABASE IF NOT EXISTS chat_app_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE chat_app_db;

-- =============================================
-- Table: users
-- Lưu trữ thông tin người dùng
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    status_message VARCHAR(255),
    status ENUM('ONLINE', 'OFFLINE', 'AWAY', 'BUSY') DEFAULT 'OFFLINE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: friends
-- Lưu trữ mối quan hệ bạn bè (hai chiều)
-- =============================================
CREATE TABLE IF NOT EXISTS friends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_friendship (user_id, friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: friend_requests
-- Lưu trữ lời mời kết bạn
-- =============================================
CREATE TABLE IF NOT EXISTS friend_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_request (sender_id, receiver_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: groups
-- Lưu trữ thông tin nhóm chat
-- =============================================
CREATE TABLE IF NOT EXISTS `groups` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_creator_id (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: group_members
-- Lưu trữ thành viên của nhóm
-- =============================================
CREATE TABLE IF NOT EXISTS group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_membership (group_id, user_id),
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: messages
-- Lưu trữ tin nhắn (cả 1:1 và nhóm)
-- =============================================
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT,  -- NULL nếu là tin nhắn nhóm
    group_id BIGINT,     -- NULL nếu là tin nhắn 1:1
    message_type ENUM('TEXT', 'IMAGE', 'FILE', 'EMOJI') DEFAULT 'TEXT',
    content TEXT,
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_group_id (group_id),
    INDEX idx_created_at (created_at),
    CHECK (
        (receiver_id IS NOT NULL AND group_id IS NULL) OR
        (receiver_id IS NULL AND group_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: call_logs (Optional - để lưu lịch sử cuộc gọi)
-- =============================================
CREATE TABLE IF NOT EXISTS call_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_id VARCHAR(100) NOT NULL UNIQUE,
    caller_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    call_type ENUM('VOICE', 'VIDEO') NOT NULL,
    status ENUM('INITIATED', 'ANSWERED', 'REJECTED', 'ENDED') DEFAULT 'INITIATED',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    duration_seconds INT,
    FOREIGN KEY (caller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_caller_id (caller_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_call_id (call_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Sample Data (for testing)
-- =============================================

-- Insert sample users (password: password123)
-- Password hash for 'password123' using BCrypt
INSERT INTO users (username, email, password_hash, full_name, status) VALUES
('alice', 'alice@example.com', '$2a$10$YourBcryptHashHere', 'Alice Johnson', 'OFFLINE'),
('bob', 'bob@example.com', '$2a$10$YourBcryptHashHere', 'Bob Smith', 'OFFLINE'),
('charlie', 'charlie@example.com', '$2a$10$YourBcryptHashHere', 'Charlie Brown', 'OFFLINE');

-- =============================================
-- Views (for convenience)
-- =============================================

-- View: friend_list_view
-- Hiển thị danh sách bạn bè với thông tin đầy đủ
CREATE OR REPLACE VIEW friend_list_view AS
SELECT
    f.user_id,
    u.id AS friend_id,
    u.username AS friend_username,
    u.full_name AS friend_full_name,
    u.status AS friend_status,
    u.status_message AS friend_status_message,
    u.last_seen AS friend_last_seen,
    f.created_at AS friends_since
FROM friends f
JOIN users u ON f.friend_id = u.id;

-- View: pending_friend_requests_view
-- Hiển thị lời mời kết bạn đang chờ
CREATE OR REPLACE VIEW pending_friend_requests_view AS
SELECT
    fr.id,
    fr.sender_id,
    sender.username AS sender_username,
    sender.full_name AS sender_full_name,
    fr.receiver_id,
    receiver.username AS receiver_username,
    receiver.full_name AS receiver_full_name,
    fr.created_at
FROM friend_requests fr
JOIN users sender ON fr.sender_id = sender.id
JOIN users receiver ON fr.receiver_id = receiver.id
WHERE fr.status = 'PENDING';

-- View: group_members_view
-- Hiển thị thành viên nhóm với thông tin đầy đủ
CREATE OR REPLACE VIEW group_members_view AS
SELECT
    gm.group_id,
    g.name AS group_name,
    gm.user_id,
    u.username,
    u.full_name,
    u.status,
    gm.joined_at
FROM group_members gm
JOIN `groups` g ON gm.group_id = g.id
JOIN users u ON gm.user_id = u.id;

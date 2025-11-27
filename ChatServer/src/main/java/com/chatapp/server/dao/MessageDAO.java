package com.chatapp.server.dao;

import com.chatapp.common.enums.MessageType;
import com.chatapp.common.model.Message;
import com.chatapp.server.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class để thao tác với bảng messages
 */
public class MessageDAO {
    private final DatabaseManager dbManager;
    private final UserDAO userDAO;

    public MessageDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.userDAO = new UserDAO();
    }

    /**
     * Lưu tin nhắn 1:1
     */
    public Message savePrivateMessage(Long senderId, Long receiverId, String content, MessageType messageType)
            throws SQLException, InterruptedException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content, message_type) VALUES (?, ?, ?, ?)";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            stmt.setString(3, content);
            stmt.setString(4, messageType.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating message failed");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return getMessageById(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating message failed, no ID obtained");
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Lưu tin nhắn nhóm
     */
    public Message saveGroupMessage(Long senderId, Long groupId, String content, MessageType messageType)
            throws SQLException, InterruptedException {
        String sql = "INSERT INTO messages (sender_id, group_id, content, message_type) VALUES (?, ?, ?, ?)";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, groupId);
            stmt.setString(3, content);
            stmt.setString(4, messageType.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating message failed");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return getMessageById(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating message failed, no ID obtained");
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Lưu tin nhắn file/hình ảnh
     */
    public Message saveFileMessage(Message message) throws SQLException, InterruptedException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, group_id, content, message_type, " +
                     "file_url, file_name, file_size) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, message.getSenderId());

            if (message.getReceiverId() != null) {
                stmt.setLong(2, message.getReceiverId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }

            if (message.getGroupId() != null) {
                stmt.setLong(3, message.getGroupId());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }

            stmt.setString(4, message.getContent());
            stmt.setString(5, message.getMessageType().name());
            stmt.setString(6, message.getFileUrl());
            stmt.setString(7, message.getFileName());
            stmt.setLong(8, message.getFileSize());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating message failed");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return getMessageById(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating message failed, no ID obtained");
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Lấy lịch sử tin nhắn 1:1
     */
    public List<Message> getPrivateMessageHistory(Long userId1, Long userId2, int limit)
            throws SQLException, InterruptedException {
        String sql = "SELECT * FROM messages " +
                     "WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
                     "ORDER BY created_at DESC LIMIT ?";
        Connection conn = dbManager.getConnection();
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId1);
            stmt.setLong(2, userId2);
            stmt.setLong(3, userId2);
            stmt.setLong(4, userId1);
            stmt.setInt(5, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return messages;
    }

    /**
     * Lấy lịch sử tin nhắn nhóm
     */
    public List<Message> getGroupMessageHistory(Long groupId, int limit)
            throws SQLException, InterruptedException {
        String sql = "SELECT * FROM messages WHERE group_id = ? ORDER BY created_at DESC LIMIT ?";
        Connection conn = dbManager.getConnection();
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return messages;
    }

    /**
     * Lấy message theo ID
     */
    public Message getMessageById(Long messageId) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM messages WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    public boolean markMessageAsRead(Long messageId) throws SQLException, InterruptedException {
        String sql = "UPDATE messages SET is_read = TRUE WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            return stmt.executeUpdate() > 0;
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Map ResultSet to Message object
     */
    private Message mapResultSetToMessage(ResultSet rs) throws SQLException, InterruptedException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setSenderId(rs.getLong("sender_id"));

        long receiverId = rs.getLong("receiver_id");
        if (!rs.wasNull()) {
            message.setReceiverId(receiverId);
        }

        long groupId = rs.getLong("group_id");
        if (!rs.wasNull()) {
            message.setGroupId(groupId);
        }

        message.setMessageType(MessageType.valueOf(rs.getString("message_type")));
        message.setContent(rs.getString("content"));
        message.setFileUrl(rs.getString("file_url"));
        message.setFileName(rs.getString("file_name"));

        long fileSize = rs.getLong("file_size");
        if (!rs.wasNull()) {
            message.setFileSize(fileSize);
        }

        message.setRead(rs.getBoolean("is_read"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            message.setTimestamp(createdAt.toLocalDateTime());
        }

        // Lấy username của sender
        var sender = userDAO.getUserById(message.getSenderId());
        if (sender != null) {
            message.setSenderUsername(sender.getUsername());
        }

        return message;
    }
}

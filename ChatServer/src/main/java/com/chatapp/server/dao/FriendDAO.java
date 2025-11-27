package com.chatapp.server.dao;

import com.chatapp.common.enums.FriendRequestStatus;
import com.chatapp.common.model.FriendRequest;
import com.chatapp.common.model.User;
import com.chatapp.server.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class để thao tác với bảng friends và friend_requests
 */
public class FriendDAO {
    private final DatabaseManager dbManager;
    private final UserDAO userDAO;

    public FriendDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.userDAO = new UserDAO();
    }

    /**
     * Gửi lời mời kết bạn
     */
    public FriendRequest sendFriendRequest(Long senderId, Long receiverId) throws SQLException, InterruptedException {
        // Kiểm tra đã là bạn bè chưa
        if (areFriends(senderId, receiverId)) {
            throw new SQLException("Already friends");
        }

        // Kiểm tra đã có request chưa
        if (hasPendingRequest(senderId, receiverId)) {
            throw new SQLException("Friend request already sent");
        }

        String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, ?)";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            stmt.setString(3, FriendRequestStatus.PENDING.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating friend request failed");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return getFriendRequestById(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating friend request failed, no ID obtained");
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Chấp nhận lời mời kết bạn
     */
    public boolean acceptFriendRequest(Long requestId) throws SQLException, InterruptedException {
        FriendRequest request = getFriendRequestById(requestId);
        if (request == null || request.getStatus() != FriendRequestStatus.PENDING) {
            return false;
        }

        Connection conn = dbManager.getConnection();
        try {
            conn.setAutoCommit(false);

            // Cập nhật trạng thái request
            String updateSql = "UPDATE friend_requests SET status = ?, responded_at = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, FriendRequestStatus.ACCEPTED.name());
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setLong(3, requestId);
                stmt.executeUpdate();
            }

            // Thêm quan hệ bạn bè (hai chiều)
            String insertSql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                // User -> Friend
                stmt.setLong(1, request.getReceiverId());
                stmt.setLong(2, request.getSenderId());
                stmt.executeUpdate();

                // Friend -> User
                stmt.setLong(1, request.getSenderId());
                stmt.setLong(2, request.getReceiverId());
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Từ chối lời mời kết bạn
     */
    public boolean rejectFriendRequest(Long requestId) throws SQLException, InterruptedException {
        String sql = "UPDATE friend_requests SET status = ?, responded_at = ? WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, FriendRequestStatus.REJECTED.name());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(3, requestId);
            return stmt.executeUpdate() > 0;
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Lấy danh sách bạn bè của user
     */
    public List<User> getFriendList(Long userId) throws SQLException, InterruptedException {
        String sql = "SELECT u.* FROM users u " +
                     "INNER JOIN friends f ON u.id = f.friend_id " +
                     "WHERE f.user_id = ? ORDER BY u.username";
        Connection conn = dbManager.getConnection();
        List<User> friends = new ArrayList<>();
        UserDAO userDAO = new UserDAO();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    friends.add(userDAO.getUserById(rs.getLong("id")));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return friends;
    }

    /**
     * Lấy danh sách lời mời kết bạn đến (pending)
     */
    public List<FriendRequest> getPendingFriendRequests(Long userId) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM friend_requests WHERE receiver_id = ? AND status = ? ORDER BY created_at DESC";
        Connection conn = dbManager.getConnection();
        List<FriendRequest> requests = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, FriendRequestStatus.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToFriendRequest(rs));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return requests;
    }

    /**
     * Lấy friend request theo ID
     */
    public FriendRequest getFriendRequestById(Long requestId) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM friend_requests WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFriendRequest(rs);
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    /**
     * Kiểm tra hai user đã là bạn bè chưa
     */
    public boolean areFriends(Long userId1, Long userId2) throws SQLException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId1);
            stmt.setLong(2, userId2);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return false;
    }

    /**
     * Kiểm tra đã có pending request chưa
     */
    private boolean hasPendingRequest(Long senderId, Long receiverId) throws SQLException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM friend_requests " +
                     "WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
                     "AND status = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            stmt.setLong(3, receiverId);
            stmt.setLong(4, senderId);
            stmt.setString(5, FriendRequestStatus.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return false;
    }

    /**
     * Map ResultSet to FriendRequest object
     */
    private FriendRequest mapResultSetToFriendRequest(ResultSet rs) throws SQLException, InterruptedException {
        FriendRequest request = new FriendRequest();
        request.setId(rs.getLong("id"));
        request.setSenderId(rs.getLong("sender_id"));
        request.setReceiverId(rs.getLong("receiver_id"));
        request.setStatus(FriendRequestStatus.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            request.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp respondedAt = rs.getTimestamp("responded_at");
        if (respondedAt != null) {
            request.setRespondedAt(respondedAt.toLocalDateTime());
        }

        // Lấy thông tin sender và receiver
        User sender = userDAO.getUserById(request.getSenderId());
        if (sender != null) {
            request.setSenderUsername(sender.getUsername());
            request.setSenderFullName(sender.getFullName());
        }

        User receiver = userDAO.getUserById(request.getReceiverId());
        if (receiver != null) {
            request.setReceiverUsername(receiver.getUsername());
        }

        return request;
    }
}

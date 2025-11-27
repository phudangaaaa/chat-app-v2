package com.chatapp.server.dao;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.chatapp.common.enums.UserStatus;
import com.chatapp.common.model.User;
import com.chatapp.server.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class để thao tác với bảng users
 */
public class UserDAO {
    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Tạo user mới (đăng ký)
     */
    public User createUser(String username, String email, String password, String fullName) throws SQLException, InterruptedException {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String sql = "INSERT INTO users (username, email, password_hash, full_name, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = dbManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, fullName);
            stmt.setString(5, UserStatus.OFFLINE.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return getUserById(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Xác thực user (đăng nhập)
     */
    public User authenticateUser(String username, String password) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String passwordHash = rs.getString("password_hash");
                    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), passwordHash);

                    if (result.verified) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    /**
     * Lấy user theo ID
     */
    public User getUserById(Long userId) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    /**
     * Lấy user theo username
     */
    public User getUserByUsername(String username) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    /**
     * Tìm kiếm users theo username hoặc email
     */
    public List<User> searchUsers(String searchTerm, Long excludeUserId) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM users WHERE (username LIKE ? OR email LIKE ?) AND id != ? LIMIT 50";
        Connection conn = dbManager.getConnection();
        List<User> users = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setLong(3, excludeUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return users;
    }

    /**
     * Cập nhật profile user
     */
    public boolean updateProfile(Long userId, String fullName, String statusMessage) throws SQLException, InterruptedException {
        String sql = "UPDATE users SET full_name = ?, status_message = ? WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, statusMessage);
            stmt.setLong(3, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Cập nhật trạng thái user
     */
    public boolean updateStatus(Long userId, UserStatus status) throws SQLException, InterruptedException {
        String sql = "UPDATE users SET status = ?, last_seen = ? WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(3, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Kiểm tra username đã tồn tại
     */
    public boolean usernameExists(String username) throws SQLException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
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
     * Kiểm tra email đã tồn tại
     */
    public boolean emailExists(String email) throws SQLException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
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
     * Map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setStatusMessage(rs.getString("status_message"));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastSeen = rs.getTimestamp("last_seen");
        if (lastSeen != null) {
            user.setLastSeen(lastSeen.toLocalDateTime());
        }

        return user;
    }
}

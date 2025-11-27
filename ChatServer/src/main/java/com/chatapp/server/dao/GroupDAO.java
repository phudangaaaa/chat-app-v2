package com.chatapp.server.dao;

import com.chatapp.common.model.Group;
import com.chatapp.common.model.User;
import com.chatapp.server.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class để thao tác với bảng groups và group_members
 */
public class GroupDAO {
    private final DatabaseManager dbManager;
    private final UserDAO userDAO;

    public GroupDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.userDAO = new UserDAO();
    }

    /**
     * Tạo nhóm mới
     */
    public Group createGroup(String name, Long creatorId, List<Long> memberIds)
            throws SQLException, InterruptedException {
        Connection conn = dbManager.getConnection();

        try {
            conn.setAutoCommit(false);

            // Tạo nhóm
            String insertGroupSql = "INSERT INTO `groups` (name, creator_id) VALUES (?, ?)";
            long groupId;

            try (PreparedStatement stmt = conn.prepareStatement(insertGroupSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setLong(2, creatorId);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating group failed");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        groupId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating group failed, no ID obtained");
                    }
                }
            }

            // Thêm creator vào nhóm
            addMemberToGroup(conn, groupId, creatorId);

            // Thêm các thành viên khác
            if (memberIds != null) {
                for (Long memberId : memberIds) {
                    if (!memberId.equals(creatorId)) {
                        addMemberToGroup(conn, groupId, memberId);
                    }
                }
            }

            conn.commit();
            return getGroupById(groupId);

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Lấy group theo ID
     */
    public Group getGroupById(Long groupId) throws SQLException, InterruptedException {
        String sql = "SELECT * FROM `groups` WHERE id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Group group = mapResultSetToGroup(rs);
                    // Load members
                    loadGroupMembers(group);
                    return group;
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return null;
    }

    /**
     * Lấy danh sách nhóm của user
     */
    public List<Group> getUserGroups(Long userId) throws SQLException, InterruptedException {
        String sql = "SELECT g.* FROM `groups` g " +
                     "INNER JOIN group_members gm ON g.id = gm.group_id " +
                     "WHERE gm.user_id = ? ORDER BY g.created_at DESC";
        Connection conn = dbManager.getConnection();
        List<Group> groups = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Group group = mapResultSetToGroup(rs);
                    loadGroupMembers(group);
                    groups.add(group);
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return groups;
    }

    /**
     * Thêm thành viên vào nhóm
     */
    public boolean addMember(Long groupId, Long userId) throws SQLException, InterruptedException {
        Connection conn = dbManager.getConnection();
        try {
            addMemberToGroup(conn, groupId, userId);
            return true;
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Xóa thành viên khỏi nhóm
     */
    public boolean removeMember(Long groupId, Long userId) throws SQLException, InterruptedException {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Lấy danh sách thành viên của nhóm
     */
    public List<User> getGroupMembers(Long groupId) throws SQLException, InterruptedException {
        String sql = "SELECT u.* FROM users u " +
                     "INNER JOIN group_members gm ON u.id = gm.user_id " +
                     "WHERE gm.group_id = ? ORDER BY u.username";
        Connection conn = dbManager.getConnection();
        List<User> members = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(userDAO.getUserById(rs.getLong("id")));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return members;
    }

    /**
     * Kiểm tra user có phải là thành viên của nhóm
     */
    public boolean isMember(Long groupId, Long userId) throws SQLException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            stmt.setLong(2, userId);
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
     * Helper method: Thêm member vào group (dùng connection có sẵn)
     */
    private void addMemberToGroup(Connection conn, Long groupId, Long userId) throws SQLException {
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Helper method: Load members vào Group object
     */
    private void loadGroupMembers(Group group) throws SQLException, InterruptedException {
        List<User> members = getGroupMembers(group.getId());
        group.getMemberIds().clear();
        group.getMemberUsernames().clear();

        for (User member : members) {
            group.addMember(member.getId(), member.getUsername());
        }
    }

    /**
     * Map ResultSet to Group object
     */
    private Group mapResultSetToGroup(ResultSet rs) throws SQLException, InterruptedException {
        Group group = new Group();
        group.setId(rs.getLong("id"));
        group.setName(rs.getString("name"));
        group.setCreatorId(rs.getLong("creator_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            group.setCreatedAt(createdAt.toLocalDateTime());
        }

        // Lấy username của creator
        User creator = userDAO.getUserById(group.getCreatorId());
        if (creator != null) {
            group.setCreatorUsername(creator.getUsername());
        }

        return group;
    }
}

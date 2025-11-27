package com.chatapp.server.service;

import com.chatapp.common.enums.PacketType;
import com.chatapp.common.model.Group;
import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.server.ChatServer;
import com.chatapp.server.dao.GroupDAO;
import com.chatapp.server.handler.ClientHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GroupService xử lý quản lý nhóm
 */
public class GroupService {
    private final ChatServer server;
    private final GroupDAO groupDAO;

    public GroupService(ChatServer server) {
        this.server = server;
        this.groupDAO = new GroupDAO();
    }

    /**
     * Xử lý tạo nhóm mới
     */
    public Packet handleCreateGroup(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.CREATE_GROUP_RESPONSE, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            String groupName = (String) data.get("name");
            @SuppressWarnings("unchecked")
            List<Number> memberIdsNum = (List<Number>) data.get("memberIds");

            if (groupName == null || groupName.trim().isEmpty()) {
                return Packet.error(PacketType.CREATE_GROUP_RESPONSE, "Group name is required");
            }

            // Convert Number to Long
            List<Long> memberIds = new ArrayList<>();
            if (memberIdsNum != null) {
                for (Number num : memberIdsNum) {
                    memberIds.add(num.longValue());
                }
            }

            // Kiểm tra có ít nhất 2 thành viên (bao gồm creator)
            if (memberIds.isEmpty()) {
                return Packet.error(PacketType.CREATE_GROUP_RESPONSE, "At least one member is required");
            }

            // Tạo nhóm
            Group group = groupDAO.createGroup(groupName, user.getId(), memberIds);

            // Thông báo cho các thành viên
            for (Long memberId : memberIds) {
                if (!memberId.equals(user.getId()) && server.isUserOnline(memberId)) {
                    server.sendGroupNotification(memberId, group, "You have been added to group: " + groupName);
                }
            }

            System.out.println("Group created: " + groupName + " by " + user.getUsername());
            return Packet.success(PacketType.CREATE_GROUP_RESPONSE, group, "Group created successfully");

        } catch (Exception e) {
            System.err.println("Error in handleCreateGroup: " + e.getMessage());
            return Packet.error(PacketType.CREATE_GROUP_RESPONSE, "Failed to create group: " + e.getMessage());
        }
    }

    /**
     * Xử lý lấy danh sách nhóm
     */
    public Packet handleGetGroupList(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.GET_GROUP_LIST_RESPONSE, "Not logged in");
            }

            List<Group> groups = groupDAO.getUserGroups(user.getId());
            return Packet.success(PacketType.GET_GROUP_LIST_RESPONSE, groups);

        } catch (Exception e) {
            System.err.println("Error in handleGetGroupList: " + e.getMessage());
            return Packet.error(PacketType.GET_GROUP_LIST_RESPONSE, "Failed to get groups: " + e.getMessage());
        }
    }

    /**
     * Xử lý tham gia nhóm
     */
    public Packet handleJoinGroup(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long groupId = ((Number) data.get("groupId")).longValue();

            boolean success = groupDAO.addMember(groupId, user.getId());
            if (success) {
                Group group = groupDAO.getGroupById(groupId);
                System.out.println("User joined group: " + user.getUsername() + " -> " + group.getName());
                return Packet.success(PacketType.SUCCESS, group, "Joined group successfully");
            } else {
                return Packet.error(PacketType.ERROR, "Failed to join group");
            }

        } catch (Exception e) {
            System.err.println("Error in handleJoinGroup: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to join group: " + e.getMessage());
        }
    }

    /**
     * Xử lý rời nhóm
     */
    public Packet handleLeaveGroup(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long groupId = ((Number) data.get("groupId")).longValue();

            boolean success = groupDAO.removeMember(groupId, user.getId());
            if (success) {
                System.out.println("User left group: " + user.getUsername());
                return Packet.success(PacketType.SUCCESS, null, "Left group successfully");
            } else {
                return Packet.error(PacketType.ERROR, "Failed to leave group");
            }

        } catch (Exception e) {
            System.err.println("Error in handleLeaveGroup: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to leave group: " + e.getMessage());
        }
    }

    /**
     * Xử lý thêm thành viên vào nhóm
     */
    public Packet handleAddMember(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long groupId = ((Number) data.get("groupId")).longValue();
            Long userId = ((Number) data.get("userId")).longValue();

            // Kiểm tra user hiện tại có phải là thành viên của nhóm không
            if (!groupDAO.isMember(groupId, user.getId())) {
                return Packet.error(PacketType.ERROR, "You are not a member of this group");
            }

            boolean success = groupDAO.addMember(groupId, userId);
            if (success) {
                Group group = groupDAO.getGroupById(groupId);

                // Thông báo cho user được thêm
                if (server.isUserOnline(userId)) {
                    server.sendGroupNotification(userId, group, "You have been added to group: " + group.getName());
                }

                System.out.println("Member added to group: User#" + userId + " -> Group#" + groupId);
                return Packet.success(PacketType.SUCCESS, group, "Member added successfully");
            } else {
                return Packet.error(PacketType.ERROR, "Failed to add member");
            }

        } catch (Exception e) {
            System.err.println("Error in handleAddMember: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to add member: " + e.getMessage());
        }
    }
}

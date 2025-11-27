package com.chatapp.server.service;

import com.chatapp.common.enums.PacketType;
import com.chatapp.common.model.FriendRequest;
import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.server.ChatServer;
import com.chatapp.server.dao.FriendDAO;
import com.chatapp.server.dao.UserDAO;
import com.chatapp.server.handler.ClientHandler;

import java.util.List;
import java.util.Map;

/**
 * FriendService xử lý quản lý bạn bè
 */
public class FriendService {
    private final ChatServer server;
    private final FriendDAO friendDAO;
    private final UserDAO userDAO;

    public FriendService(ChatServer server) {
        this.server = server;
        this.friendDAO = new FriendDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Xử lý tìm kiếm người dùng
     */
    public Packet handleSearchUsers(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.SEARCH_USER_RESPONSE, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) packet.getData();
            String searchTerm = data.get("searchTerm");

            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return Packet.error(PacketType.SEARCH_USER_RESPONSE, "Search term is required");
            }

            List<User> users = userDAO.searchUsers(searchTerm, user.getId());
            return Packet.success(PacketType.SEARCH_USER_RESPONSE, users);

        } catch (Exception e) {
            System.err.println("Error in handleSearchUsers: " + e.getMessage());
            return Packet.error(PacketType.SEARCH_USER_RESPONSE, "Search failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý gửi lời mời kết bạn
     */
    public Packet handleSendFriendRequest(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long receiverId = ((Number) data.get("receiverId")).longValue();

            // Check if receiver exists
            User receiver = userDAO.getUserById(receiverId);
            if (receiver == null) {
                return Packet.error(PacketType.ERROR, "User not found");
            }

            // Send friend request
            FriendRequest request = friendDAO.sendFriendRequest(user.getId(), receiverId);

            // Notify receiver if online
            if (server.isUserOnline(receiverId)) {
                server.sendFriendRequestNotification(receiverId, request);
            }

            System.out.println("Friend request sent: " + user.getUsername() + " -> " + receiver.getUsername());
            return Packet.success(PacketType.SUCCESS, request, "Friend request sent");

        } catch (Exception e) {
            System.err.println("Error in handleSendFriendRequest: " + e.getMessage());
            return Packet.error(PacketType.ERROR, e.getMessage());
        }
    }

    /**
     * Xử lý chấp nhận lời mời kết bạn
     */
    public Packet handleAcceptFriendRequest(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long requestId = ((Number) data.get("requestId")).longValue();

            boolean success = friendDAO.acceptFriendRequest(requestId);
            if (success) {
                System.out.println("Friend request accepted: " + requestId);
                return Packet.success(PacketType.SUCCESS, null, "Friend request accepted");
            } else {
                return Packet.error(PacketType.ERROR, "Failed to accept friend request");
            }

        } catch (Exception e) {
            System.err.println("Error in handleAcceptFriendRequest: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to accept: " + e.getMessage());
        }
    }

    /**
     * Xử lý từ chối lời mời kết bạn
     */
    public Packet handleRejectFriendRequest(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long requestId = ((Number) data.get("requestId")).longValue();

            boolean success = friendDAO.rejectFriendRequest(requestId);
            if (success) {
                System.out.println("Friend request rejected: " + requestId);
                return Packet.success(PacketType.SUCCESS, null, "Friend request rejected");
            } else {
                return Packet.error(PacketType.ERROR, "Failed to reject friend request");
            }

        } catch (Exception e) {
            System.err.println("Error in handleRejectFriendRequest: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to reject: " + e.getMessage());
        }
    }

    /**
     * Xử lý lấy danh sách bạn bè
     */
    public Packet handleGetFriendList(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.GET_FRIEND_LIST_RESPONSE, "Not logged in");
            }

            List<User> friends = friendDAO.getFriendList(user.getId());
            return Packet.success(PacketType.GET_FRIEND_LIST_RESPONSE, friends);

        } catch (Exception e) {
            System.err.println("Error in handleGetFriendList: " + e.getMessage());
            return Packet.error(PacketType.GET_FRIEND_LIST_RESPONSE, "Failed to get friend list: " + e.getMessage());
        }
    }

    /**
     * Xử lý lấy danh sách lời mời kết bạn
     */
    public Packet handleGetFriendRequests(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.GET_FRIEND_REQUESTS_RESPONSE, "Not logged in");
            }

            List<FriendRequest> requests = friendDAO.getPendingFriendRequests(user.getId());
            return Packet.success(PacketType.GET_FRIEND_REQUESTS_RESPONSE, requests);

        } catch (Exception e) {
            System.err.println("Error in handleGetFriendRequests: " + e.getMessage());
            return Packet.error(PacketType.GET_FRIEND_REQUESTS_RESPONSE, "Failed to get requests: " + e.getMessage());
        }
    }

    /**
     * Xử lý lấy profile người dùng
     */
    public Packet handleGetUserProfile(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.GET_USER_PROFILE_RESPONSE, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long userId = ((Number) data.get("userId")).longValue();

            User targetUser = userDAO.getUserById(userId);
            if (targetUser == null) {
                return Packet.error(PacketType.GET_USER_PROFILE_RESPONSE, "User not found");
            }

            return Packet.success(PacketType.GET_USER_PROFILE_RESPONSE, targetUser);

        } catch (Exception e) {
            System.err.println("Error in handleGetUserProfile: " + e.getMessage());
            return Packet.error(PacketType.GET_USER_PROFILE_RESPONSE, "Failed to get profile: " + e.getMessage());
        }
    }
}

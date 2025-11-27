package com.chatapp.server.service;

import com.chatapp.common.enums.PacketType;
import com.chatapp.common.enums.UserStatus;
import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.server.ChatServer;
import com.chatapp.server.dao.UserDAO;
import com.chatapp.server.handler.ClientHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthService xử lý authentication và user management
 */
public class AuthService {
    private final ChatServer server;
    private final UserDAO userDAO;

    public AuthService(ChatServer server) {
        this.server = server;
        this.userDAO = new UserDAO();
    }

    /**
     * Xử lý đăng ký user mới
     */
    public Packet handleRegister(Packet packet, ClientHandler handler) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) packet.getData();
            String username = data.get("username");
            String email = data.get("email");
            String password = data.get("password");
            String fullName = data.get("fullName");

            // Validation
            if (username == null || username.trim().isEmpty()) {
                return Packet.error(PacketType.REGISTER_RESPONSE, "Username is required");
            }
            if (email == null || email.trim().isEmpty()) {
                return Packet.error(PacketType.REGISTER_RESPONSE, "Email is required");
            }
            if (password == null || password.length() < 6) {
                return Packet.error(PacketType.REGISTER_RESPONSE, "Password must be at least 6 characters");
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                return Packet.error(PacketType.REGISTER_RESPONSE, "Full name is required");
            }

            // Check if username exists
            if (userDAO.usernameExists(username)) {
                return Packet.error(PacketType.REGISTER_RESPONSE, "Username already exists");
            }

            // Check if email exists
            if (userDAO.emailExists(email)) {
                return Packet.error(PacketType.REGISTER_RESPONSE, "Email already exists");
            }

            // Create user
            User user = userDAO.createUser(username, email, password, fullName);
            System.out.println("New user registered: " + username);

            return Packet.success(PacketType.REGISTER_RESPONSE, user, "Registration successful");

        } catch (Exception e) {
            System.err.println("Error in handleRegister: " + e.getMessage());
            return Packet.error(PacketType.REGISTER_RESPONSE, "Registration failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý đăng nhập
     */
    public Packet handleLogin(Packet packet, ClientHandler handler) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) packet.getData();
            String username = data.get("username");
            String password = data.get("password");

            // Validation
            if (username == null || username.trim().isEmpty()) {
                return Packet.error(PacketType.LOGIN_RESPONSE, "Username is required");
            }
            if (password == null || password.trim().isEmpty()) {
                return Packet.error(PacketType.LOGIN_RESPONSE, "Password is required");
            }

            // Authenticate
            User user = userDAO.authenticateUser(username, password);
            if (user == null) {
                return Packet.error(PacketType.LOGIN_RESPONSE, "Invalid username or password");
            }

            // Check if already logged in
            if (server.isUserOnline(user.getId())) {
                return Packet.error(PacketType.LOGIN_RESPONSE, "User already logged in");
            }

            // Update status to ONLINE
            userDAO.updateStatus(user.getId(), UserStatus.ONLINE);
            user.setStatus(UserStatus.ONLINE);

            // Add to active clients
            handler.setCurrentUser(user);
            server.addClient(user, handler);

            // Notify friends about status change
            server.notifyStatusChange(user.getId(), UserStatus.ONLINE);

            System.out.println("User logged in: " + username);
            return Packet.success(PacketType.LOGIN_RESPONSE, user, "Login successful");

        } catch (Exception e) {
            System.err.println("Error in handleLogin: " + e.getMessage());
            return Packet.error(PacketType.LOGIN_RESPONSE, "Login failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý đăng xuất
     */
    public Packet handleLogout(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            // Update status to OFFLINE
            userDAO.updateStatus(user.getId(), UserStatus.OFFLINE);

            // Notify friends
            server.notifyStatusChange(user.getId(), UserStatus.OFFLINE);

            System.out.println("User logged out: " + user.getUsername());
            return Packet.success(PacketType.SUCCESS, null, "Logout successful");

        } catch (Exception e) {
            System.err.println("Error in handleLogout: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Logout failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý cập nhật profile
     */
    public Packet handleUpdateProfile(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.UPDATE_PROFILE_RESPONSE, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) packet.getData();
            String fullName = data.get("fullName");
            String statusMessage = data.get("statusMessage");

            if (fullName != null && !fullName.trim().isEmpty()) {
                userDAO.updateProfile(user.getId(), fullName, statusMessage);
                user.setFullName(fullName);
                user.setStatusMessage(statusMessage);

                System.out.println("Profile updated for: " + user.getUsername());
                return Packet.success(PacketType.UPDATE_PROFILE_RESPONSE, user, "Profile updated successfully");
            } else {
                return Packet.error(PacketType.UPDATE_PROFILE_RESPONSE, "Full name is required");
            }

        } catch (Exception e) {
            System.err.println("Error in handleUpdateProfile: " + e.getMessage());
            return Packet.error(PacketType.UPDATE_PROFILE_RESPONSE, "Update failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý cập nhật trạng thái
     */
    public Packet handleUpdateStatus(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.UPDATE_STATUS_RESPONSE, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) packet.getData();
            String statusStr = data.get("status");

            UserStatus newStatus = UserStatus.valueOf(statusStr);
            userDAO.updateStatus(user.getId(), newStatus);
            user.setStatus(newStatus);

            // Notify friends
            server.notifyStatusChange(user.getId(), newStatus);

            System.out.println("Status updated for " + user.getUsername() + ": " + newStatus);
            return Packet.success(PacketType.UPDATE_STATUS_RESPONSE, user, "Status updated successfully");

        } catch (Exception e) {
            System.err.println("Error in handleUpdateStatus: " + e.getMessage());
            return Packet.error(PacketType.UPDATE_STATUS_RESPONSE, "Update failed: " + e.getMessage());
        }
    }

    /**
     * Set user offline (called when disconnecting)
     */
    public void setUserOffline(Long userId) {
        try {
            userDAO.updateStatus(userId, UserStatus.OFFLINE);
        } catch (Exception e) {
            System.err.println("Error setting user offline: " + e.getMessage());
        }
    }
}

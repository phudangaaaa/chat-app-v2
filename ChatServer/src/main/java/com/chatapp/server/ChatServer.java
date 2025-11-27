package com.chatapp.server;

import com.chatapp.common.enums.PacketType;
import com.chatapp.common.enums.UserStatus;
import com.chatapp.common.model.Message;
import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.server.dao.FriendDAO;
import com.chatapp.server.dao.UserDAO;
import com.chatapp.server.handler.ClientHandler;
import com.chatapp.server.util.ConfigManager;
import com.chatapp.server.util.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatServer - Server chính của ứng dụng chat
 */
public class ChatServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final Map<Long, ClientHandler> activeClients; // userId -> ClientHandler
    private boolean running;
    private final ConfigManager configManager;
    private final DatabaseManager dbManager;

    public ChatServer() {
        this.activeClients = new ConcurrentHashMap<>();
        this.configManager = ConfigManager.getInstance();
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Khởi động server
     */
    public void start() {
        int port = configManager.getServerPort();
        int threadPoolSize = configManager.getThreadPoolSize();

        try {
            // Test database connection
            if (!dbManager.testConnection()) {
                System.err.println("Failed to connect to database. Please check your database configuration.");
                return;
            }
            System.out.println("Database connection successful");

            // Initialize thread pool
            threadPool = Executors.newFixedThreadPool(threadPoolSize);
            System.out.println("Thread pool initialized with " + threadPoolSize + " threads");

            // Start server socket
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Chat Server started on port " + port);
            System.out.println("Waiting for clients...");

            // Accept client connections
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                threadPool.execute(clientHandler);
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Dừng server
     */
    public void stop() {
        running = false;
        System.out.println("Shutting down server...");

        // Disconnect all clients
        for (ClientHandler client : activeClients.values()) {
            client.disconnect();
        }
        activeClients.clear();

        // Shutdown thread pool
        if (threadPool != null) {
            threadPool.shutdown();
        }

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // Close database connections
        dbManager.closeAllConnections();

        System.out.println("Server stopped");
    }

    /**
     * Thêm client vào danh sách active clients
     */
    public void addClient(User user, ClientHandler handler) {
        activeClients.put(user.getId(), handler);
        System.out.println("Active clients: " + activeClients.size());
    }

    /**
     * Xóa client khỏi danh sách active clients
     */
    public void removeClient(ClientHandler handler) {
        if (handler.getCurrentUser() != null) {
            activeClients.remove(handler.getCurrentUser().getId());
            System.out.println("Active clients: " + activeClients.size());
        }
    }

    /**
     * Lấy ClientHandler theo userId
     */
    public ClientHandler getClientHandler(Long userId) {
        return activeClients.get(userId);
    }

    /**
     * Kiểm tra user có đang online không
     */
    public boolean isUserOnline(Long userId) {
        return activeClients.containsKey(userId);
    }

    /**
     * Gửi packet đến một user cụ thể
     */
    public void sendToUser(Long userId, Packet packet) {
        ClientHandler handler = activeClients.get(userId);
        if (handler != null && handler.isRunning()) {
            handler.sendPacket(packet);
        }
    }

    /**
     * Gửi tin nhắn đến user (notification)
     */
    public void sendMessageToUser(Long userId, Message message) {
        Packet packet = Packet.success(PacketType.MESSAGE_NOTIFICATION, message);
        sendToUser(userId, packet);
    }

    /**
     * Gửi tin nhắn đến tất cả thành viên trong nhóm
     */
    public void sendMessageToGroup(Long groupId, Message message, Long excludeUserId) {
        try {
            // Lấy danh sách thành viên nhóm
            var groupDAO = new com.chatapp.server.dao.GroupDAO();
            var members = groupDAO.getGroupMembers(groupId);

            Packet packet = Packet.success(PacketType.MESSAGE_NOTIFICATION, message);

            for (User member : members) {
                if (!member.getId().equals(excludeUserId)) {
                    sendToUser(member.getId(), packet);
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending message to group: " + e.getMessage());
        }
    }

    /**
     * Thông báo thay đổi trạng thái đến tất cả bạn bè
     */
    public void notifyStatusChange(Long userId, UserStatus newStatus) {
        try {
            FriendDAO friendDAO = new FriendDAO();
            UserDAO userDAO = new UserDAO();

            List<User> friends = friendDAO.getFriendList(userId);
            User user = userDAO.getUserById(userId);

            if (user != null) {
                user.setStatus(newStatus);
                Packet notification = Packet.success(PacketType.STATUS_CHANGE_NOTIFICATION, user);

                for (User friend : friends) {
                    sendToUser(friend.getId(), notification);
                }
            }
        } catch (Exception e) {
            System.err.println("Error notifying status change: " + e.getMessage());
        }
    }

    /**
     * Gửi thông báo friend request
     */
    public void sendFriendRequestNotification(Long receiverId, Object friendRequest) {
        Packet packet = Packet.success(PacketType.FRIEND_REQUEST_NOTIFICATION, friendRequest);
        sendToUser(receiverId, packet);
    }

    /**
     * Gửi thông báo nhóm
     */
    public void sendGroupNotification(Long userId, Object data, String message) {
        Packet packet = Packet.success(PacketType.GROUP_NOTIFICATION, data, message);
        sendToUser(userId, packet);
    }

    /**
     * Gửi thông báo cuộc gọi
     */
    public void sendCallNotification(Long userId, Object callSignal) {
        Packet packet = Packet.success(PacketType.CALL_NOTIFICATION, callSignal);
        sendToUser(userId, packet);
    }

    /**
     * Broadcast message to all connected clients (for testing)
     */
    public void broadcast(Packet packet) {
        for (ClientHandler client : activeClients.values()) {
            client.sendPacket(packet);
        }
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received");
            server.stop();
        }));

        server.start();
    }

    // Getters
    public Map<Long, ClientHandler> getActiveClients() {
        return activeClients;
    }
}

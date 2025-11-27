package com.chatapp.server.handler;

import com.chatapp.server.enums.PacketType;
import com.chatapp.server.enums.UserStatus;
import com.chatapp.server.model.User;
import com.chatapp.server.protocol.Packet;
import com.chatapp.server.ChatServer;
import com.chatapp.server.service.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * ClientHandler xử lý từng kết nối client trong một thread riêng
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private User currentUser;
    private boolean running;

    // Services
    private final AuthService authService;
    private final FriendService friendService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final FileTransferService fileTransferService;
    private final CallService callService;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.running = true;

        // Initialize services
        this.authService = new AuthService(server);
        this.friendService = new FriendService(server);
        this.messageService = new MessageService(server);
        this.groupService = new GroupService(server);
        this.fileTransferService = new FileTransferService(server);
        this.callService = new CallService(server);

        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error creating client handler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());

        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handlePacket(line);
            }
        } catch (SocketException e) {
            System.out.println("Client disconnected: " +
                (currentUser != null ? currentUser.getUsername() : socket.getInetAddress().getHostAddress()));
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Xử lý packet nhận được từ client
     */
    private void handlePacket(String json) {
        try {
            Packet packet = Packet.fromJson(json);
            Packet response;

            switch (packet.getType()) {
                // Authentication
                case REGISTER_REQUEST:
                    response = authService.handleRegister(packet, this);
                    break;
                case LOGIN_REQUEST:
                    response = authService.handleLogin(packet, this);
                    break;
                case LOGOUT_REQUEST:
                    response = authService.handleLogout(packet, this);
                    break;

                // User Management
                case UPDATE_PROFILE_REQUEST:
                    response = authService.handleUpdateProfile(packet, this);
                    break;
                case UPDATE_STATUS_REQUEST:
                    response = authService.handleUpdateStatus(packet, this);
                    break;

                // Friend Management
                case SEARCH_USER_REQUEST:
                    response = friendService.handleSearchUsers(packet, this);
                    break;
                case SEND_FRIEND_REQUEST:
                    response = friendService.handleSendFriendRequest(packet, this);
                    break;
                case ACCEPT_FRIEND_REQUEST:
                    response = friendService.handleAcceptFriendRequest(packet, this);
                    break;
                case REJECT_FRIEND_REQUEST:
                    response = friendService.handleRejectFriendRequest(packet, this);
                    break;
                case GET_FRIEND_LIST_REQUEST:
                    response = friendService.handleGetFriendList(packet, this);
                    break;
                case GET_FRIEND_REQUESTS_REQUEST:
                    response = friendService.handleGetFriendRequests(packet, this);
                    break;
                case GET_USER_PROFILE_REQUEST:
                    response = friendService.handleGetUserProfile(packet, this);
                    break;

                // Messaging
                case SEND_MESSAGE:
                    response = messageService.handleSendMessage(packet, this);
                    break;
                case GET_MESSAGE_HISTORY_REQUEST:
                    response = messageService.handleGetMessageHistory(packet, this);
                    break;

                // Group Management
                case CREATE_GROUP_REQUEST:
                    response = groupService.handleCreateGroup(packet, this);
                    break;
                case GET_GROUP_LIST_REQUEST:
                    response = groupService.handleGetGroupList(packet, this);
                    break;
                case JOIN_GROUP_REQUEST:
                    response = groupService.handleJoinGroup(packet, this);
                    break;
                case LEAVE_GROUP_REQUEST:
                    response = groupService.handleLeaveGroup(packet, this);
                    break;
                case ADD_MEMBER_TO_GROUP_REQUEST:
                    response = groupService.handleAddMember(packet, this);
                    break;

                // File Transfer
                case FILE_TRANSFER_REQUEST:
                    response = fileTransferService.handleFileTransferRequest(packet, this);
                    break;
                case FILE_CHUNK:
                    response = fileTransferService.handleFileChunk(packet, this);
                    break;

                // Call Signaling
                case CALL_OFFER:
                    response = callService.handleCallOffer(packet, this);
                    break;
                case CALL_ANSWER:
                    response = callService.handleCallAnswer(packet, this);
                    break;
                case CALL_REJECT:
                    response = callService.handleCallReject(packet, this);
                    break;
                case CALL_END:
                    response = callService.handleCallEnd(packet, this);
                    break;

                // Heartbeat
                case HEARTBEAT:
                    response = Packet.success(PacketType.HEARTBEAT, null);
                    break;

                default:
                    response = Packet.error(PacketType.ERROR, "Unknown packet type");
                    break;
            }

            if (response != null) {
                sendPacket(response);
            }

        } catch (Exception e) {
            System.err.println("Error handling packet: " + e.getMessage());
            e.printStackTrace();
            sendPacket(Packet.error(PacketType.ERROR, "Server error: " + e.getMessage()));
        }
    }

    /**
     * Gửi packet đến client
     */
    public void sendPacket(Packet packet) {
        if (writer != null && !socket.isClosed()) {
            writer.println(packet.toJson());
        }
    }

    /**
     * Ngắt kết nối client
     */
    public void disconnect() {
        running = false;

        // Update user status to OFFLINE
        if (currentUser != null) {
            try {
                authService.setUserOffline(currentUser.getId());
                server.notifyStatusChange(currentUser.getId(), UserStatus.OFFLINE);
            } catch (Exception e) {
                System.err.println("Error updating user status: " + e.getMessage());
            }
        }

        // Remove from active clients
        server.removeClient(this);

        // Close resources
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }

        System.out.println("Client disconnected: " +
            (currentUser != null ? currentUser.getUsername() : "Unknown"));
    }

    // Getters and Setters
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isRunning() {
        return running && !socket.isClosed();
    }
}

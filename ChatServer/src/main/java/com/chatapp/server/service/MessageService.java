package com.chatapp.server.service;

import com.chatapp.server.enums.MessageType;
import com.chatapp.server.enums.PacketType;
import com.chatapp.server.model.Message;
import com.chatapp.server.model.User;
import com.chatapp.server.protocol.Packet;
import com.chatapp.server.ChatServer;
import com.chatapp.server.dao.MessageDAO;
import com.chatapp.server.handler.ClientHandler;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

/**
 * MessageService xử lý tin nhắn
 */
public class MessageService {
    private final ChatServer server;
    private final MessageDAO messageDAO;
    private final Gson gson;

    public MessageService(ChatServer server) {
        this.server = server;
        this.messageDAO = new MessageDAO();
        this.gson = new Gson();
    }

    /**
     * Xử lý gửi tin nhắn
     */
    public Packet handleSendMessage(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            // Parse message from packet data
            String json = gson.toJson(packet.getData());
            Message message = gson.fromJson(json, Message.class);

            message.setSenderId(user.getId());
            message.setSenderUsername(user.getUsername());

            Message savedMessage;

            // Xử lý tin nhắn 1:1
            if (message.getReceiverId() != null) {
                savedMessage = messageDAO.savePrivateMessage(
                    message.getSenderId(),
                    message.getReceiverId(),
                    message.getContent(),
                    message.getMessageType()
                );

                // Gửi tin nhắn đến người nhận nếu đang online
                if (server.isUserOnline(message.getReceiverId())) {
                    server.sendMessageToUser(message.getReceiverId(), savedMessage);
                }

                System.out.println("Private message sent: " + user.getUsername() + " -> User#" + message.getReceiverId());
            }
            // Xử lý tin nhắn nhóm
            else if (message.getGroupId() != null) {
                savedMessage = messageDAO.saveGroupMessage(
                    message.getSenderId(),
                    message.getGroupId(),
                    message.getContent(),
                    message.getMessageType()
                );

                // Gửi tin nhắn đến tất cả thành viên nhóm
                server.sendMessageToGroup(message.getGroupId(), savedMessage, user.getId());

                System.out.println("Group message sent: " + user.getUsername() + " -> Group#" + message.getGroupId());
            } else {
                return Packet.error(PacketType.ERROR, "Invalid message: missing receiver or group");
            }

            return Packet.success(PacketType.SUCCESS, savedMessage, "Message sent");

        } catch (Exception e) {
            System.err.println("Error in handleSendMessage: " + e.getMessage());
            e.printStackTrace();
            return Packet.error(PacketType.ERROR, "Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Xử lý lấy lịch sử tin nhắn
     */
    public Packet handleGetMessageHistory(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.GET_MESSAGE_HISTORY_RESPONSE, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();

            List<Message> messages;

            // Lịch sử tin nhắn 1:1
            if (data.containsKey("userId")) {
                Long otherUserId = ((Number) data.get("userId")).longValue();
                int limit = data.containsKey("limit") ? ((Number) data.get("limit")).intValue() : 50;

                messages = messageDAO.getPrivateMessageHistory(user.getId(), otherUserId, limit);
                System.out.println("Message history retrieved for users: " + user.getId() + " <-> " + otherUserId);
            }
            // Lịch sử tin nhắn nhóm
            else if (data.containsKey("groupId")) {
                Long groupId = ((Number) data.get("groupId")).longValue();
                int limit = data.containsKey("limit") ? ((Number) data.get("limit")).intValue() : 50;

                messages = messageDAO.getGroupMessageHistory(groupId, limit);
                System.out.println("Message history retrieved for group: " + groupId);
            } else {
                return Packet.error(PacketType.GET_MESSAGE_HISTORY_RESPONSE, "Invalid request: missing userId or groupId");
            }

            return Packet.success(PacketType.GET_MESSAGE_HISTORY_RESPONSE, messages);

        } catch (Exception e) {
            System.err.println("Error in handleGetMessageHistory: " + e.getMessage());
            return Packet.error(PacketType.GET_MESSAGE_HISTORY_RESPONSE, "Failed to get history: " + e.getMessage());
        }
    }
}

package com.chatapp.server.service;

import com.chatapp.server.enums.CallType;
import com.chatapp.server.enums.PacketType;
import com.chatapp.server.model.CallSignal;
import com.chatapp.server.model.User;
import com.chatapp.server.protocol.Packet;
import com.chatapp.server.ChatServer;
import com.chatapp.server.handler.ClientHandler;
import com.google.gson.Gson;

import java.util.Map;
import java.util.UUID;

/**
 * CallService xử lý signaling cho cuộc gọi voice/video
 * Lưu ý: Đây chỉ là signaling cơ bản, không bao gồm media streaming thực tế
 */
public class CallService {
    private final ChatServer server;
    private final Gson gson;

    public CallService(ChatServer server) {
        this.server = server;
        this.gson = new Gson();
    }

    /**
     * Xử lý yêu cầu cuộc gọi (Call Offer)
     */
    public Packet handleCallOffer(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            Long receiverId = ((Number) data.get("receiverId")).longValue();
            String callTypeStr = (String) data.get("callType");
            CallType callType = CallType.valueOf(callTypeStr);

            // Check if receiver is online
            if (!server.isUserOnline(receiverId)) {
                return Packet.error(PacketType.ERROR, "User is not online");
            }

            // Create call signal
            String callId = UUID.randomUUID().toString();
            CallSignal callSignal = new CallSignal(callId, user.getId(), receiverId, callType);
            callSignal.setCallerUsername(user.getUsername());

            // Send call notification to receiver
            server.sendCallNotification(receiverId, callSignal);

            System.out.println("Call initiated: " + user.getUsername() + " -> User#" + receiverId +
                             " (" + callType + ")");

            return Packet.success(PacketType.SUCCESS, callSignal, "Call initiated");

        } catch (Exception e) {
            System.err.println("Error in handleCallOffer: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to initiate call: " + e.getMessage());
        }
    }

    /**
     * Xử lý chấp nhận cuộc gọi (Call Answer)
     */
    public Packet handleCallAnswer(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            String json = gson.toJson(packet.getData());
            CallSignal callSignal = gson.fromJson(json, CallSignal.class);

            // Notify caller that call was accepted
            Packet notification = new Packet(PacketType.CALL_ANSWER);
            notification.setData(callSignal);
            notification.setSuccess(true);
            notification.setMessage("Call accepted");

            server.sendToUser(callSignal.getCallerId(), notification);

            System.out.println("Call accepted: CallId=" + callSignal.getCallId());
            return Packet.success(PacketType.SUCCESS, null, "Call accepted");

        } catch (Exception e) {
            System.err.println("Error in handleCallAnswer: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to accept call: " + e.getMessage());
        }
    }

    /**
     * Xử lý từ chối cuộc gọi (Call Reject)
     */
    public Packet handleCallReject(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            String json = gson.toJson(packet.getData());
            CallSignal callSignal = gson.fromJson(json, CallSignal.class);

            // Notify caller that call was rejected
            Packet notification = new Packet(PacketType.CALL_REJECT);
            notification.setData(callSignal);
            notification.setSuccess(true);
            notification.setMessage("Call rejected");

            server.sendToUser(callSignal.getCallerId(), notification);

            System.out.println("Call rejected: CallId=" + callSignal.getCallId());
            return Packet.success(PacketType.SUCCESS, null, "Call rejected");

        } catch (Exception e) {
            System.err.println("Error in handleCallReject: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to reject call: " + e.getMessage());
        }
    }

    /**
     * Xử lý kết thúc cuộc gọi (Call End)
     */
    public Packet handleCallEnd(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            String json = gson.toJson(packet.getData());
            CallSignal callSignal = gson.fromJson(json, CallSignal.class);

            // Notify the other party that call has ended
            Long otherPartyId = callSignal.getCallerId().equals(user.getId()) ?
                              callSignal.getReceiverId() : callSignal.getCallerId();

            Packet notification = new Packet(PacketType.CALL_END);
            notification.setData(callSignal);
            notification.setSuccess(true);
            notification.setMessage("Call ended");

            server.sendToUser(otherPartyId, notification);

            System.out.println("Call ended: CallId=" + callSignal.getCallId());
            return Packet.success(PacketType.SUCCESS, null, "Call ended");

        } catch (Exception e) {
            System.err.println("Error in handleCallEnd: " + e.getMessage());
            return Packet.error(PacketType.ERROR, "Failed to end call: " + e.getMessage());
        }
    }
}

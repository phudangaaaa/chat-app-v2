package com.chatapp.client.model;

import com.chatapp.client.enums.FriendRequestStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model class đại diện cho lời mời kết bạn
 */
public class FriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private Long receiverId;
    private String receiverUsername;
    private FriendRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public FriendRequest() {
        this.status = FriendRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public FriendRequest(Long senderId, Long receiverId) {
        this();
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public void setSenderFullName(String senderFullName) {
        this.senderFullName = senderFullName;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    @Override
    public String toString() {
        return "FriendRequest{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", status=" + status +
                '}';
    }
}

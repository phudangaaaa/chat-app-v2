package com.chatapp.server.model;

import com.chatapp.server.enums.MessageType;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model class đại diện cho tin nhắn
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long senderId;
    private String senderUsername;
    private Long receiverId;        // Null nếu là tin nhắn nhóm
    private Long groupId;           // Null nếu là tin nhắn 1:1
    private MessageType messageType;
    private String content;
    private String fileUrl;         // URL hoặc đường dẫn file (cho IMAGE/FILE)
    private String fileName;
    private Long fileSize;
    private LocalDateTime timestamp;
    private boolean isRead;

    public Message() {
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
        this.messageType = MessageType.TEXT;
    }

    public Message(Long senderId, String content, MessageType messageType) {
        this();
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
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

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", senderUsername='" + senderUsername + '\'' +
                ", messageType=" + messageType +
                ", timestamp=" + timestamp +
                '}';
    }
}

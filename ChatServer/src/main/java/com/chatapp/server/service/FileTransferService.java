package com.chatapp.server.service;

import com.chatapp.server.enums.MessageType;
import com.chatapp.server.enums.PacketType;
import com.chatapp.server.model.FileChunk;
import com.chatapp.server.model.Message;
import com.chatapp.server.model.User;
import com.chatapp.server.protocol.Packet;
import com.chatapp.server.ChatServer;
import com.chatapp.server.dao.MessageDAO;
import com.chatapp.server.handler.ClientHandler;
import com.chatapp.server.util.ConfigManager;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileTransferService xử lý truyền file
 */
public class FileTransferService {
    private final ChatServer server;
    private final MessageDAO messageDAO;
    private final Gson gson;
    private final String uploadDir;
    private final Map<String, FileTransferSession> activeSessions;

    public FileTransferService(ChatServer server) {
        this.server = server;
        this.messageDAO = new MessageDAO();
        this.gson = new Gson();
        this.uploadDir = ConfigManager.getInstance().getFileUploadDir();
        this.activeSessions = new ConcurrentHashMap<>();

        // Create upload directory if not exists
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            System.err.println("Error creating upload directory: " + e.getMessage());
        }
    }

    /**
     * Xử lý yêu cầu truyền file
     */
    public Packet handleFileTransferRequest(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.FILE_TRANSFER_RESPONSE, "Not logged in");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) packet.getData();
            String fileName = (String) data.get("fileName");
            Long fileSize = ((Number) data.get("fileSize")).longValue();
            Long receiverId = data.get("receiverId") != null ? ((Number) data.get("receiverId")).longValue() : null;
            Long groupId = data.get("groupId") != null ? ((Number) data.get("groupId")).longValue() : null;

            // Generate unique file ID
            String fileId = UUID.randomUUID().toString();

            // Create session
            FileTransferSession session = new FileTransferSession(
                fileId, fileName, fileSize, user.getId(), receiverId, groupId
            );
            activeSessions.put(fileId, session);

            Map<String, Object> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("chunkSize", ConfigManager.getInstance().getFileChunkSize());

            System.out.println("File transfer initiated: " + fileName + " (ID: " + fileId + ")");
            return Packet.success(PacketType.FILE_TRANSFER_RESPONSE, response);

        } catch (Exception e) {
            System.err.println("Error in handleFileTransferRequest: " + e.getMessage());
            return Packet.error(PacketType.FILE_TRANSFER_RESPONSE, "Failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý nhận file chunk
     */
    public Packet handleFileChunk(Packet packet, ClientHandler handler) {
        try {
            User user = handler.getCurrentUser();
            if (user == null) {
                return Packet.error(PacketType.ERROR, "Not logged in");
            }

            String json = gson.toJson(packet.getData());
            FileChunk chunk = gson.fromJson(json, FileChunk.class);

            FileTransferSession session = activeSessions.get(chunk.getFileId());
            if (session == null) {
                return Packet.error(PacketType.ERROR, "Invalid file transfer session");
            }

            // Decode base64 data
            byte[] data = Base64.getDecoder().decode(new String(chunk.getData()));
            session.writeChunk(data);

            // Check if transfer complete
            if (chunk.getChunkNumber() == chunk.getTotalChunks() - 1) {
                session.close();
                String filePath = session.getFilePath();

                // Save message to database
                Message message = new Message();
                message.setSenderId(session.getSenderId());
                message.setSenderUsername(user.getUsername());
                message.setReceiverId(session.getReceiverId());
                message.setGroupId(session.getGroupId());
                message.setMessageType(isImageFile(session.getFileName()) ? MessageType.IMAGE : MessageType.FILE);
                message.setContent("File: " + session.getFileName());
                message.setFileUrl(filePath);
                message.setFileName(session.getFileName());
                message.setFileSize(session.getFileSize());

                Message savedMessage = messageDAO.saveFileMessage(message);

                // Send to receiver(s)
                if (session.getReceiverId() != null) {
                    if (server.isUserOnline(session.getReceiverId())) {
                        server.sendMessageToUser(session.getReceiverId(), savedMessage);
                    }
                } else if (session.getGroupId() != null) {
                    server.sendMessageToGroup(session.getGroupId(), savedMessage, user.getId());
                }

                activeSessions.remove(chunk.getFileId());

                System.out.println("File transfer completed: " + session.getFileName());
                return Packet.success(PacketType.FILE_TRANSFER_COMPLETE, savedMessage, "File uploaded successfully");
            }

            return Packet.success(PacketType.SUCCESS, null, "Chunk received");

        } catch (Exception e) {
            System.err.println("Error in handleFileChunk: " + e.getMessage());
            e.printStackTrace();
            return Packet.error(PacketType.ERROR, "Failed to receive chunk: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra file có phải là hình ảnh không
     */
    private boolean isImageFile(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("bmp");
    }

    /**
     * File transfer session class
     */
    private class FileTransferSession {
        private final String fileId;
        private final String fileName;
        private final long fileSize;
        private final Long senderId;
        private final Long receiverId;
        private final Long groupId;
        private final String filePath;
        private FileOutputStream outputStream;

        public FileTransferSession(String fileId, String fileName, long fileSize,
                                   Long senderId, Long receiverId, Long groupId) throws IOException {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.groupId = groupId;

            // Create unique file path
            String uniqueFileName = fileId + "_" + fileName;
            this.filePath = uploadDir + File.separator + uniqueFileName;

            // Create output stream
            this.outputStream = new FileOutputStream(filePath);
        }

        public void writeChunk(byte[] data) throws IOException {
            outputStream.write(data);
        }

        public void close() throws IOException {
            if (outputStream != null) {
                outputStream.close();
            }
        }

        public String getFileId() {
            return fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public Long getSenderId() {
            return senderId;
        }

        public Long getReceiverId() {
            return receiverId;
        }

        public Long getGroupId() {
            return groupId;
        }

        public String getFilePath() {
            return filePath;
        }
    }
}

package com.chatapp.server.model;

import java.io.Serializable;

/**
 * Model class đại diện cho một phần của file khi truyền tải
 */
public class FileChunk implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileId;          // ID duy nhất của file transfer
    private String fileName;
    private long totalSize;
    private int chunkNumber;        // Số thứ tự của chunk
    private int totalChunks;        // Tổng số chunks
    private byte[] data;            // Dữ liệu của chunk
    private Long senderId;
    private Long receiverId;
    private Long groupId;

    public FileChunk() {
    }

    public FileChunk(String fileId, String fileName, long totalSize, int chunkNumber, int totalChunks, byte[] data) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.totalSize = totalSize;
        this.chunkNumber = chunkNumber;
        this.totalChunks = totalChunks;
        this.data = data;
    }

    // Getters and Setters
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(int chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
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

    @Override
    public String toString() {
        return "FileChunk{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", chunkNumber=" + chunkNumber +
                "/" + totalChunks +
                '}';
    }
}

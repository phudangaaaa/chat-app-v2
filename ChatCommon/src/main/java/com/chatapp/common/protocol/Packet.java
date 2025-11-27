package com.chatapp.common.protocol;

import com.chatapp.common.enums.PacketType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Packet class để trao đổi dữ liệu giữa Client và Server
 * Sử dụng JSON để serialize/deserialize
 */
public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private PacketType type;
    private Object data;
    private boolean success;
    private String message;
    private LocalDateTime timestamp;

    public Packet() {
        this.timestamp = LocalDateTime.now();
    }

    public Packet(PacketType type) {
        this();
        this.type = type;
    }

    public Packet(PacketType type, Object data) {
        this(type);
        this.data = data;
    }

    // Static factory methods
    public static Packet success(PacketType type, Object data, String message) {
        Packet packet = new Packet(type, data);
        packet.setSuccess(true);
        packet.setMessage(message);
        return packet;
    }

    public static Packet success(PacketType type, Object data) {
        return success(type, data, null);
    }

    public static Packet error(PacketType type, String message) {
        Packet packet = new Packet(type);
        packet.setSuccess(false);
        packet.setMessage(message);
        return packet;
    }

    // JSON serialization methods
    public String toJson() {
        return gson.toJson(this);
    }

    public static Packet fromJson(String json) {
        return gson.fromJson(json, Packet.class);
    }

    public <T> T getData(Class<T> clazz) {
        if (data == null) {
            return null;
        }
        // Convert LinkedTreeMap to target class if needed
        return gson.fromJson(gson.toJson(data), clazz);
    }

    // Getters and Setters
    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "type=" + type +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

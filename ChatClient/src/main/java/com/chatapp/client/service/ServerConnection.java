package com.chatapp.client.service;

import com.chatapp.client.enums.PacketType;
import com.chatapp.client.protocol.Packet;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ServerConnection quản lý kết nối Socket với Server
 */
public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected;
    private Thread listenerThread;

    private String serverHost;
    private int serverPort;

    // Callback handlers cho các loại packet
    private final Map<PacketType, Consumer<Packet>> packetHandlers;
    private Consumer<Packet> defaultHandler;

    private ServerConnection() {
        this.packetHandlers = new ConcurrentHashMap<>();
        loadConfiguration();
    }

    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    /**
     * Load configuration từ file
     */
    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("client.properties")) {
            if (input != null) {
                props.load(input);
                this.serverHost = props.getProperty("server.host", "localhost");
                this.serverPort = Integer.parseInt(props.getProperty("server.port", "8888"));
            } else {
                this.serverHost = "localhost";
                this.serverPort = 8888;
            }
        } catch (IOException e) {
            System.err.println("Error loading client configuration: " + e.getMessage());
            this.serverHost = "localhost";
            this.serverPort = 8888;
        }
    }

    /**
     * Kết nối đến server
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            // Start listener thread
            startListenerThread();

            System.out.println("Connected to server: " + serverHost + ":" + serverPort);
            return true;

        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    /**
     * Ngắt kết nối
     */
    public void disconnect() {
        connected = false;

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        System.out.println("Disconnected from server");
    }

    /**
     * Gửi packet đến server
     */
    public void sendPacket(Packet packet) {
        if (connected && writer != null) {
            writer.println(packet.toJson());
        } else {
            System.err.println("Not connected to server");
        }
    }

    /**
     * Start listener thread để nhận packet từ server
     */
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = reader.readLine()) != null) {
                    Packet packet = Packet.fromJson(line);
                    handleIncomingPacket(packet);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Connection lost: " + e.getMessage());
                    Platform.runLater(() -> {
                        // Notify UI about disconnection
                        if (defaultHandler != null) {
                            Packet errorPacket = Packet.error(PacketType.ERROR, "Connection lost");
                            defaultHandler.accept(errorPacket);
                        }
                    });
                }
            } finally {
                connected = false;
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Xử lý packet nhận được từ server
     */
    private void handleIncomingPacket(Packet packet) {
        Platform.runLater(() -> {
            Consumer<Packet> handler = packetHandlers.get(packet.getType());
            if (handler != null) {
                handler.accept(packet);
            } else if (defaultHandler != null) {
                defaultHandler.accept(packet);
            } else {
                System.out.println("Received packet: " + packet.getType());
            }
        });
    }

    /**
     * Đăng ký handler cho một loại packet cụ thể
     */
    public void registerHandler(PacketType type, Consumer<Packet> handler) {
        packetHandlers.put(type, handler);
    }

    /**
     * Xóa handler
     */
    public void unregisterHandler(PacketType type) {
        packetHandlers.remove(type);
    }

    /**
     * Đăng ký default handler cho các packet không có handler riêng
     */
    public void setDefaultHandler(Consumer<Packet> handler) {
        this.defaultHandler = handler;
    }

    // Getters
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}

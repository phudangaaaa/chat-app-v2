package com.chatapp.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager quản lý cấu hình server
 */
public class ConfigManager {
    private static ConfigManager instance;
    private Properties properties;

    private ConfigManager() {
        properties = new Properties();
        loadConfiguration();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (input == null) {
                System.out.println("Unable to find server.properties, using default values");
                setDefaultProperties();
                return;
            }
            properties.load(input);
            System.out.println("Server configuration loaded successfully");
        } catch (IOException e) {
            System.err.println("Error loading server configuration: " + e.getMessage());
            setDefaultProperties();
        }
    }

    private void setDefaultProperties() {
        properties.setProperty("server.port", "8888");
        properties.setProperty("server.thread.pool.size", "50");
        properties.setProperty("file.upload.dir", "uploads/");
        properties.setProperty("file.chunk.size", "65536");
    }

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8888"));
    }

    public int getThreadPoolSize() {
        return Integer.parseInt(properties.getProperty("server.thread.pool.size", "50"));
    }

    public String getFileUploadDir() {
        return properties.getProperty("file.upload.dir", "uploads/");
    }

    public int getFileChunkSize() {
        return Integer.parseInt(properties.getProperty("file.chunk.size", "65536"));
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}

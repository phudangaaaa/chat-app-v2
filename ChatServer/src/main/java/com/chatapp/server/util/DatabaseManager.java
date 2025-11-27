package com.chatapp.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * DatabaseManager quản lý Connection Pool đến MySQL database
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private BlockingQueue<Connection> connectionPool;
    private String url;
    private String username;
    private String password;
    private int poolSize;

    private DatabaseManager() {
        loadConfiguration();
        initializeConnectionPool();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (input == null) {
                System.out.println("Unable to find server.properties, using default values");
                setDefaultConfiguration();
                return;
            }
            props.load(input);
            this.url = props.getProperty("db.url");
            this.username = props.getProperty("db.username");
            this.password = props.getProperty("db.password");
            this.poolSize = Integer.parseInt(props.getProperty("db.pool.size", "20"));

            // Load driver
            Class.forName(props.getProperty("db.driver"));
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading database configuration: " + e.getMessage());
            setDefaultConfiguration();
        }
    }

    private void setDefaultConfiguration() {
        this.url = "jdbc:mysql://localhost:3306/chat_app_db?useSSL=false&serverTimezone=UTC";
        this.username = "root";
        this.password = "";
        this.poolSize = 20;
    }

    private void initializeConnectionPool() {
        connectionPool = new ArrayBlockingQueue<>(poolSize);
        try {
            for (int i = 0; i < poolSize; i++) {
                connectionPool.add(createNewConnection());
            }
            System.out.println("Database connection pool initialized with " + poolSize + " connections");
        } catch (SQLException e) {
            System.err.println("Error initializing connection pool: " + e.getMessage());
        }
    }

    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Lấy connection từ pool
     */
    public Connection getConnection() throws SQLException, InterruptedException {
        Connection connection = connectionPool.take();
        if (connection.isClosed()) {
            connection = createNewConnection();
        }
        return connection;
    }

    /**
     * Trả connection về pool
     */
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connectionPool.offer(connection);
                }
            } catch (SQLException e) {
                System.err.println("Error releasing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Đóng tất cả connections trong pool
     */
    public void closeAllConnections() {
        System.out.println("Closing all database connections...");
        for (Connection connection : connectionPool) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        connectionPool.clear();
    }

    /**
     * Test kết nối database
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            boolean isValid = conn.isValid(5);
            releaseConnection(conn);
            return isValid;
        } catch (SQLException | InterruptedException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}

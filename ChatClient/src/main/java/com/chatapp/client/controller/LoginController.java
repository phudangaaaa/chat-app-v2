package com.chatapp.client.controller;

import com.chatapp.client.ChatClientApp;
import com.chatapp.client.service.ServerConnection;
import com.chatapp.client.service.SessionManager;
import com.chatapp.client.enums.PacketType;
import com.chatapp.client.model.User;
import com.chatapp.client.protocol.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller cho Login screen
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField serverHostField;
    @FXML private TextField serverPortField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private ServerConnection serverConnection;
    private SessionManager sessionManager;

    @FXML
    public void initialize() {
        serverConnection = ServerConnection.getInstance();
        sessionManager = SessionManager.getInstance();

        // Set server settings
        serverHostField.setText(serverConnection.getServerHost());
        serverPortField.setText(String.valueOf(serverConnection.getServerPort()));
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String serverHost = serverHostField.getText().trim();
        String serverPort = serverPortField.getText().trim();

        // Validation
        if (username.isEmpty()) {
            showError("Please enter username");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter password");
            return;
        }

        // Disable login button
        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        // Update server settings
        try {
            serverConnection.setServerHost(serverHost);
            serverConnection.setServerPort(Integer.parseInt(serverPort));
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            loginButton.setDisable(false);
            return;
        }

        // Connect to server in background
        CompletableFuture.runAsync(() -> {
            if (!serverConnection.isConnected()) {
                if (!serverConnection.connect()) {
                    Platform.runLater(() -> {
                        showError("Failed to connect to server");
                        loginButton.setDisable(false);
                    });
                    return;
                }
            }

            // Send login request
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("password", password);

            Packet loginPacket = new Packet(PacketType.LOGIN_REQUEST, data);

            // Register handler for login response
            CompletableFuture<Packet> responseFuture = new CompletableFuture<>();
            serverConnection.registerHandler(PacketType.LOGIN_RESPONSE, responseFuture::complete);

            // Send packet
            serverConnection.sendPacket(loginPacket);

            // Wait for response
            try {
                Packet response = responseFuture.get(10, TimeUnit.SECONDS);
                serverConnection.unregisterHandler(PacketType.LOGIN_RESPONSE);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        User user = response.getData(User.class);
                        sessionManager.setCurrentUser(user);
                        openMainWindow();
                    } else {
                        showError(response.getMessage());
                        loginButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Login timeout or error: " + e.getMessage());
                    loginButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void handleRegisterLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Register - Chat Application");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load register screen");
        }
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Chat Application - " + sessionManager.getCurrentUsername());
            stage.setOnCloseRequest(event -> {
                serverConnection.disconnect();
                Platform.exit();
            });
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load main window");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

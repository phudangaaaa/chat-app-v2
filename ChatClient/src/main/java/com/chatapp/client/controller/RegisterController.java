package com.chatapp.client.controller;

import com.chatapp.client.service.ServerConnection;
import com.chatapp.common.enums.PacketType;
import com.chatapp.common.protocol.Packet;
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
 * Controller cho Register screen
 */
public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;

    private ServerConnection serverConnection;

    @FXML
    public void initialize() {
        serverConnection = ServerConnection.getInstance();
    }

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (fullName.isEmpty()) {
            showError("Please enter your full name");
            return;
        }
        if (username.isEmpty()) {
            showError("Please enter username");
            return;
        }
        if (email.isEmpty()) {
            showError("Please enter email");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Please enter a valid email address");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        // Disable register button
        registerButton.setDisable(true);
        errorLabel.setVisible(false);

        // Connect to server and register
        CompletableFuture.runAsync(() -> {
            if (!serverConnection.isConnected()) {
                if (!serverConnection.connect()) {
                    Platform.runLater(() -> {
                        showError("Failed to connect to server");
                        registerButton.setDisable(false);
                    });
                    return;
                }
            }

            // Send register request
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("email", email);
            data.put("password", password);
            data.put("fullName", fullName);

            Packet registerPacket = new Packet(PacketType.REGISTER_REQUEST, data);

            // Register handler for register response
            CompletableFuture<Packet> responseFuture = new CompletableFuture<>();
            serverConnection.registerHandler(PacketType.REGISTER_RESPONSE, responseFuture::complete);

            // Send packet
            serverConnection.sendPacket(registerPacket);

            // Wait for response
            try {
                Packet response = responseFuture.get(10, TimeUnit.SECONDS);
                serverConnection.unregisterHandler(PacketType.REGISTER_RESPONSE);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showSuccessAndRedirect();
                    } else {
                        showError(response.getMessage());
                        registerButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Registration timeout or error: " + e.getMessage());
                    registerButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void handleLoginLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login - Chat Application");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load login screen");
        }
    }

    private void showSuccessAndRedirect() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registration Successful");
        alert.setHeaderText(null);
        alert.setContentText("Your account has been created successfully! Please login.");
        alert.showAndWait();

        handleLoginLink();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

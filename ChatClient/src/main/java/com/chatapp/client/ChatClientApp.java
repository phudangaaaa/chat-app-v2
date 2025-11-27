package com.chatapp.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ChatClientApp - Main JavaFX Application class
 */
public class ChatClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Login - Chat Application");
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load login screen: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        System.out.println("Application shutting down...");
        // Cleanup
    }

    public static void main(String[] args) {
        launch(args);
    }
}

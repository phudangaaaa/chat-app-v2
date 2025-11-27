package com.chatapp.client.controller;

import com.chatapp.client.service.ServerConnection;
import com.chatapp.client.service.SessionManager;
import com.chatapp.common.enums.*;
import com.chatapp.common.model.*;
import com.chatapp.common.protocol.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MainController - Controller chính cho màn hình chat
 */
public class MainController {

    @FXML private Label userLabel;
    @FXML private MenuButton statusMenuButton;
    @FXML private TextField searchField;
    @FXML private TabPane tabPane;
    @FXML private ListView<User> friendListView;
    @FXML private ListView<Group> groupListView;
    @FXML private ListView<FriendRequest> requestListView;
    @FXML private VBox chatArea;
    @FXML private HBox chatHeader;
    @FXML private Label chatTitleLabel;
    @FXML private Button callButton;
    @FXML private Button videoCallButton;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageContainer;
    @FXML private VBox welcomePane;
    @FXML private HBox messageInputArea;
    @FXML private TextField messageField;

    private ServerConnection serverConnection;
    private SessionManager sessionManager;
    private User currentChatUser;
    private Group currentChatGroup;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        serverConnection = ServerConnection.getInstance();
        sessionManager = SessionManager.getInstance();

        // Set user info
        userLabel.setText(sessionManager.getCurrentUser().getFullName());

        // Configure ListViews
        configureFriendListView();
        configureGroupListView();
        configureRequestListView();

        // Register packet handlers
        registerPacketHandlers();

        // Load initial data
        loadFriendList();
        loadGroupList();
        loadFriendRequests();
    }

    private void configureFriendListView() {
        friendListView.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    // Status indicator
                    Label statusIndicator = new Label("●");
                    statusIndicator.setStyle(getStatusColor(user.getStatus()));

                    // User info
                    VBox userInfo = new VBox(2);
                    Label nameLabel = new Label(user.getFullName());
                    nameLabel.setStyle("-fx-font-weight: bold;");
                    Label statusLabel = new Label(user.getStatus().toString());
                    statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
                    userInfo.getChildren().addAll(nameLabel, statusLabel);

                    box.getChildren().addAll(statusIndicator, userInfo);
                    setGraphic(box);
                }
            }
        });
    }

    private void configureGroupListView() {
        groupListView.setCellFactory(param -> new ListCell<Group>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    Label nameLabel = new Label(group.getName());
                    nameLabel.setStyle("-fx-font-weight: bold;");
                    Label membersLabel = new Label(group.getMemberIds().size() + " members");
                    membersLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
                    box.getChildren().addAll(nameLabel, membersLabel);
                    setGraphic(box);
                }
            }
        });
    }

    private void configureRequestListView() {
        requestListView.setCellFactory(param -> new ListCell<FriendRequest>() {
            @Override
            protected void updateItem(FriendRequest request, boolean empty) {
                super.updateItem(request, empty);
                if (empty || request == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    Label nameLabel = new Label(request.getSenderFullName());
                    nameLabel.setStyle("-fx-font-weight: bold;");
                    Label usernameLabel = new Label("@" + request.getSenderUsername());
                    usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

                    HBox buttonBox = new HBox(5);
                    Button acceptBtn = new Button("Accept");
                    acceptBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 10px;");
                    acceptBtn.setOnAction(e -> acceptFriendRequest(request));

                    Button rejectBtn = new Button("Reject");
                    rejectBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 10px;");
                    rejectBtn.setOnAction(e -> rejectFriendRequest(request));

                    buttonBox.getChildren().addAll(acceptBtn, rejectBtn);
                    box.getChildren().addAll(nameLabel, usernameLabel, buttonBox);
                    setGraphic(box);
                }
            }
        });
    }

    private void registerPacketHandlers() {
        // Message notifications
        serverConnection.registerHandler(PacketType.MESSAGE_NOTIFICATION, this::handleMessageNotification);

        // Status change notifications
        serverConnection.registerHandler(PacketType.STATUS_CHANGE_NOTIFICATION, this::handleStatusChange);

        // Friend request notifications
        serverConnection.registerHandler(PacketType.FRIEND_REQUEST_NOTIFICATION, this::handleFriendRequestNotification);

        // Call notifications
        serverConnection.registerHandler(PacketType.CALL_NOTIFICATION, this::handleCallNotification);
        serverConnection.registerHandler(PacketType.CALL_ANSWER, this::handleCallAnswer);
        serverConnection.registerHandler(PacketType.CALL_REJECT, this::handleCallReject);
        serverConnection.registerHandler(PacketType.CALL_END, this::handleCallEnd);
    }

    @FXML
    private void handleFriendClick() {
        User selectedFriend = friendListView.getSelectionModel().getSelectedItem();
        if (selectedFriend != null) {
            openPrivateChat(selectedFriend);
        }
    }

    @FXML
    private void handleGroupClick() {
        Group selectedGroup = groupListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            openGroupChat(selectedGroup);
        }
    }

    private void openPrivateChat(User friend) {
        currentChatUser = friend;
        currentChatGroup = null;

        // Update UI
        welcomePane.setVisible(false);
        chatHeader.setVisible(true);
        messageScrollPane.setVisible(true);
        messageInputArea.setVisible(true);

        chatTitleLabel.setText(friend.getFullName() + " (@" + friend.getUsername() + ")");
        callButton.setVisible(true);
        videoCallButton.setVisible(true);

        // Clear and load messages
        messageContainer.getChildren().clear();
        loadMessageHistory(friend.getId(), null);
    }

    private void openGroupChat(Group group) {
        currentChatUser = null;
        currentChatGroup = group;

        // Update UI
        welcomePane.setVisible(false);
        chatHeader.setVisible(true);
        messageScrollPane.setVisible(true);
        messageInputArea.setVisible(true);

        chatTitleLabel.setText(group.getName() + " (" + group.getMemberIds().size() + " members)");
        callButton.setVisible(false);
        videoCallButton.setVisible(false);

        // Clear and load messages
        messageContainer.getChildren().clear();
        loadMessageHistory(null, group.getId());
    }

    @FXML
    private void handleSendMessage() {
        String content = messageField.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        Message message = new Message();
        message.setContent(content);
        message.setMessageType(MessageType.TEXT);

        if (currentChatUser != null) {
            message.setReceiverId(currentChatUser.getId());
        } else if (currentChatGroup != null) {
            message.setGroupId(currentChatGroup.getId());
        } else {
            return;
        }

        // Send to server
        Packet packet = new Packet(PacketType.SEND_MESSAGE, message);
        serverConnection.sendPacket(packet);

        // Clear input
        messageField.clear();

        // Add message to UI (will be echoed back from server with ID)
    }

    private void handleMessageNotification(Packet packet) {
        Message message = packet.getData(Message.class);

        // Check if message belongs to current chat
        boolean belongsToCurrentChat = false;
        if (currentChatUser != null &&
            ((message.getSenderId().equals(currentChatUser.getId()) && message.getReceiverId().equals(sessionManager.getCurrentUserId())) ||
             (message.getSenderId().equals(sessionManager.getCurrentUserId()) && message.getReceiverId().equals(currentChatUser.getId())))) {
            belongsToCurrentChat = true;
        } else if (currentChatGroup != null && message.getGroupId() != null && message.getGroupId().equals(currentChatGroup.getId())) {
            belongsToCurrentChat = true;
        }

        if (belongsToCurrentChat) {
            displayMessage(message);
        } else {
            // Show notification
            showNotification("New message from " + message.getSenderUsername());
        }
    }

    private void displayMessage(Message message) {
        boolean isOwnMessage = message.getSenderId().equals(sessionManager.getCurrentUserId());

        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        messageBox.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10));
        bubble.setMaxWidth(400);
        bubble.setStyle(isOwnMessage ?
            "-fx-background-color: #3498DB; -fx-background-radius: 10;" :
            "-fx-background-color: #ECF0F1; -fx-background-radius: 10;");

        // Sender name (for group chats)
        if (currentChatGroup != null && !isOwnMessage) {
            Label senderLabel = new Label(message.getSenderUsername());
            senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " +
                (isOwnMessage ? "white" : "#7F8C8D") + ";");
            bubble.getChildren().add(senderLabel);
        }

        // Message content
        Label contentLabel = new Label(message.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: " + (isOwnMessage ? "white" : "black") + ";");

        // Timestamp
        Label timeLabel = new Label(message.getTimestamp().format(timeFormatter));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isOwnMessage ? "#E8F4F8" : "#95A5A6") + ";");

        bubble.getChildren().addAll(contentLabel, timeLabel);
        messageBox.getChildren().add(bubble);

        messageContainer.getChildren().add(messageBox);

        // Auto-scroll to bottom
        Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    private void loadMessageHistory(Long userId, Long groupId) {
        Map<String, Object> data = new HashMap<>();
        if (userId != null) {
            data.put("userId", userId);
        } else if (groupId != null) {
            data.put("groupId", groupId);
        }
        data.put("limit", 50);

        Packet packet = new Packet(PacketType.GET_MESSAGE_HISTORY_REQUEST, data);

        CompletableFuture<Packet> future = new CompletableFuture<>();
        serverConnection.registerHandler(PacketType.GET_MESSAGE_HISTORY_RESPONSE, response -> {
            serverConnection.unregisterHandler(PacketType.GET_MESSAGE_HISTORY_RESPONSE);
            future.complete(response);
        });

        serverConnection.sendPacket(packet);

        future.thenAccept(response -> {
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> messagesData = (List<Map<String, Object>>) response.getData();
                // Parse and display messages (reverse order)
                // Implementation details...
            }
        });
    }

    private void loadFriendList() {
        Packet packet = new Packet(PacketType.GET_FRIEND_LIST_REQUEST);

        CompletableFuture<Packet> future = new CompletableFuture<>();
        serverConnection.registerHandler(PacketType.GET_FRIEND_LIST_RESPONSE, response -> {
            serverConnection.unregisterHandler(PacketType.GET_FRIEND_LIST_RESPONSE);
            future.complete(response);
        });

        serverConnection.sendPacket(packet);

        future.thenAccept(response -> {
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> friendsData = (List<Map<String, Object>>) response.getData();
                // Parse and update ListView
                // Implementation details...
            }
        });
    }

    private void loadGroupList() {
        Packet packet = new Packet(PacketType.GET_GROUP_LIST_REQUEST);
        serverConnection.sendPacket(packet);
        // Handle response...
    }

    private void loadFriendRequests() {
        Packet packet = new Packet(PacketType.GET_FRIEND_REQUESTS_REQUEST);
        serverConnection.sendPacket(packet);
        // Handle response...
    }

    // Additional handler methods...
    @FXML private void handleSearch() { /* Implementation */ }
    @FXML private void handleAddFriend() { /* Implementation */ }
    @FXML private void handleCreateGroup() { /* Implementation */ }
    @FXML private void handleRefreshRequests() { loadFriendRequests(); }
    @FXML private void handleCall() { /* Implementation */ }
    @FXML private void handleVideoCall() { /* Implementation */ }
    @FXML private void handleAttachFile() { /* Implementation */ }
    @FXML private void handleEmoji() { /* Implementation */ }
    @FXML private void setStatusOnline() { updateStatus(UserStatus.ONLINE); }
    @FXML private void setStatusAway() { updateStatus(UserStatus.AWAY); }
    @FXML private void setStatusBusy() { updateStatus(UserStatus.BUSY); }
    @FXML private void handleEditProfile() { /* Implementation */ }
    @FXML private void handleLogout() { /* Implementation */ }

    private void updateStatus(UserStatus status) {
        Map<String, String> data = new HashMap<>();
        data.put("status", status.name());
        Packet packet = new Packet(PacketType.UPDATE_STATUS_REQUEST, data);
        serverConnection.sendPacket(packet);
    }

    private void acceptFriendRequest(FriendRequest request) { /* Implementation */ }
    private void rejectFriendRequest(FriendRequest request) { /* Implementation */ }
    private void handleStatusChange(Packet packet) { /* Implementation */ }
    private void handleFriendRequestNotification(Packet packet) { /* Implementation */ }
    private void handleCallNotification(Packet packet) { /* Implementation */ }
    private void handleCallAnswer(Packet packet) { /* Implementation */ }
    private void handleCallReject(Packet packet) { /* Implementation */ }
    private void handleCallEnd(Packet packet) { /* Implementation */ }
    private void showNotification(String message) { System.out.println("Notification: " + message); }

    private String getStatusColor(UserStatus status) {
        switch (status) {
            case ONLINE: return "-fx-text-fill: #27AE60;";
            case AWAY: return "-fx-text-fill: #F39C12;";
            case BUSY: return "-fx-text-fill: #E74C3C;";
            default: return "-fx-text-fill: #95A5A6;";
        }
    }
}

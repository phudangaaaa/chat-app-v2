package com.chatapp.client.service;

import com.chatapp.client.model.User;

/**
 * SessionManager quản lý session của user hiện tại
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void clear() {
        this.currentUser = null;
    }

    public Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
}

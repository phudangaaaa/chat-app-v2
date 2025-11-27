package com.chatapp.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class đại diện cho nhóm chat
 */
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long creatorId;
    private String creatorUsername;
    private LocalDateTime createdAt;
    private List<Long> memberIds;
    private List<String> memberUsernames;

    public Group() {
        this.createdAt = LocalDateTime.now();
        this.memberIds = new ArrayList<>();
        this.memberUsernames = new ArrayList<>();
    }

    public Group(String name, Long creatorId) {
        this();
        this.name = name;
        this.creatorId = creatorId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }

    public List<String> getMemberUsernames() {
        return memberUsernames;
    }

    public void setMemberUsernames(List<String> memberUsernames) {
        this.memberUsernames = memberUsernames;
    }

    public void addMember(Long userId, String username) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
            memberUsernames.add(username);
        }
    }

    public void removeMember(Long userId) {
        int index = memberIds.indexOf(userId);
        if (index >= 0) {
            memberIds.remove(index);
            memberUsernames.remove(index);
        }
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", creatorId=" + creatorId +
                ", memberCount=" + memberIds.size() +
                '}';
    }
}

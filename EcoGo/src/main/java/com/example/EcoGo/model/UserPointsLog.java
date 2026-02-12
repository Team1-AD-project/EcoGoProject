package com.example.EcoGo.model;

import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

// import org.springframework.data.mongodb.core.mapping.Field; // Removed

import java.time.LocalDateTime;

@Document(collection = "user_points_logs")
public class UserPointsLog {

    @Id
    private String id;

    @Indexed
    private String userId; // Stores Business UserID (e.g. "user001")

    private String changeType; // gain, deduct, redeem

    private long points;

    private String source; // trip, task, redeem, admin

    private String description; // Brief description of the transaction

    private String relatedId; // Trip ID / Order ID (UUID)

    private AdminAction adminAction; // Only if source=admin

    private long balanceAfter; // Critical for audit

    private LocalDateTime createdAt;

    // Constructors
    public UserPointsLog() {
        this.createdAt = LocalDateTime.now();
    }

    public UserPointsLog(String userId, String changeType, long points, String source, String relatedId,
            long balanceAfter) {
        this.userId = userId;
        this.changeType = changeType;
        this.points = points;
        this.source = source;
        this.relatedId = relatedId;
        this.balanceAfter = balanceAfter;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public AdminAction getAdminAction() {
        return adminAction;
    }

    public void setAdminAction(AdminAction adminAction) {
        this.adminAction = adminAction;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Nested Classes
    public static class AdminAction {
        private String operatorId;
        private String reason;
        private String approvalStatus; // pending, approved, rejected

        public AdminAction() {
        }

        public AdminAction(String operatorId, String reason, String approvalStatus) {
            this.operatorId = operatorId;
            this.reason = reason;
            this.approvalStatus = approvalStatus;
        }

        public String getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(String operatorId) {
            this.operatorId = operatorId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getApprovalStatus() {
            return approvalStatus;
        }

        public void setApprovalStatus(String approvalStatus) {
            this.approvalStatus = approvalStatus;
        }
    }
}

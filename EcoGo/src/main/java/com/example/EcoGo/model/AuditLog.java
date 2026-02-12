package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String auditId;
    private String actorUserId;
    private String targetUserId;
    private String action;
    private Map<String, Object> details = new HashMap<>();
    private Instant createdAt;

    public AuditLog() {
        // Empty constructor intentionally left blank.
        // Required by Spring Data MongoDB for deserialization of documents from the database.
        // Fields are populated via setters or reflection during document instantiation.
    }

    // --- Getters/Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }

    public String getActorUserId() { return actorUserId; }
    public void setActorUserId(String actorUserId) { this.actorUserId = actorUserId; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

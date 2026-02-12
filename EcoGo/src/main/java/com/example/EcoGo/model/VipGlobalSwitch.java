package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "vip_global_switches")
public class VipGlobalSwitch {
    @Id
    private String id;

    @Indexed(unique = true)
    private String switchKey; // 唯一标识

    private String displayName; // 显示名称
    private String description; // 描述
    private boolean isEnabled; // 开关状态

    private LocalDateTime updatedAt;
    private String updatedBy;

    public VipGlobalSwitch() {
        this.updatedAt = LocalDateTime.now();
        this.isEnabled = true; // Default
    }

    public VipGlobalSwitch(String switchKey, String displayName, String description) {
        this.switchKey = switchKey;
        this.displayName = displayName;
        this.description = description;
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSwitchKey() {
        return switchKey;
    }

    public void setSwitchKey(String switchKey) {
        this.switchKey = switchKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

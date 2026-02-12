package com.example.EcoGo.dto;

import com.example.EcoGo.model.Challenge;

import java.time.LocalDateTime;

/**
 * DTO for Challenge create/update requests.
 * Replaces direct use of the persistent Challenge entity in @RequestBody (SonarQube S4684).
 */
public class ChallengeRequestDto {
    private String title;
    private String description;
    private String type;
    private Double target;
    private Integer reward;
    private String badge;
    private String icon;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Challenge toEntity() {
        Challenge challenge = new Challenge();
        challenge.setTitle(this.title);
        challenge.setDescription(this.description);
        challenge.setType(this.type);
        challenge.setTarget(this.target);
        challenge.setReward(this.reward);
        challenge.setBadge(this.badge);
        challenge.setIcon(this.icon);
        challenge.setStatus(this.status);
        challenge.setStartTime(this.startTime);
        challenge.setEndTime(this.endTime);
        return challenge;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getTarget() { return target; }
    public void setTarget(Double target) { this.target = target; }

    public Integer getReward() { return reward; }
    public void setReward(Integer reward) { this.reward = reward; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}

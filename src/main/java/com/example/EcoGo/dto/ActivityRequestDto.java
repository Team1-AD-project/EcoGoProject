package com.example.EcoGo.dto;

import com.example.EcoGo.model.Activity;

import java.time.LocalDateTime;

/**
 * DTO for Activity create/update requests.
 * Replaces direct use of the persistent Activity entity in @RequestBody (SonarQube S4684).
 */
public class ActivityRequestDto {
    private String title;
    private String description;
    private String type;
    private String status;
    private Integer rewardCredits;
    private Integer maxParticipants;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double latitude;
    private Double longitude;
    private String locationName;

    public Activity toEntity() {
        Activity activity = new Activity();
        activity.setTitle(this.title);
        activity.setDescription(this.description);
        activity.setType(this.type);
        activity.setStatus(this.status);
        activity.setRewardCredits(this.rewardCredits);
        activity.setMaxParticipants(this.maxParticipants);
        activity.setStartTime(this.startTime);
        activity.setEndTime(this.endTime);
        activity.setLatitude(this.latitude);
        activity.setLongitude(this.longitude);
        activity.setLocationName(this.locationName);
        return activity;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRewardCredits() { return rewardCredits; }
    public void setRewardCredits(Integer rewardCredits) { this.rewardCredits = rewardCredits; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
}

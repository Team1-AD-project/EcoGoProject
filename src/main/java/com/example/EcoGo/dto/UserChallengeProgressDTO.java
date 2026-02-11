package com.example.EcoGo.dto;

import java.time.LocalDateTime;

/**
 * 用户挑战进度DTO
 * 包含从Trip表计算的实时进度
 */
public class UserChallengeProgressDTO {
    private String id;
    private String challengeId;
    private String userId;
    private String status; // IN_PROGRESS, COMPLETED
    private Double current; // 实时计算的进度值
    private Double target; // 目标值（从Challenge获取）
    private Double progressPercent; // 进度百分比
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;
    private Boolean rewardClaimed;

    // 用户信息字段
    private String userNickname; // 用户昵称
    private String userEmail; // 用户邮箱
    private String userAvatar; // 用户头像

    // Default constructor used by service layer to build progress response
    public UserChallengeProgressDTO() {
        // Empty constructor intentionally left blank.
        // Used by service layer for dynamic construction of DTO objects.
        // Fields are populated via setter methods after instantiation.
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCurrent() {
        return current;
    }

    public void setCurrent(Double current) {
        this.current = current;
    }

    public Double getTarget() {
        return target;
    }

    public void setTarget(Double target) {
        this.target = target;
    }

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Boolean getRewardClaimed() {
        return rewardClaimed;
    }

    public void setRewardClaimed(Boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
}

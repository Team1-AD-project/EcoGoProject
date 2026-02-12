package com.example.EcoGo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 用户挑战进度实体
 * 记录用户参与挑战的信息
 *
 * 注意：current进度从Trip表实时计算，不存储
 */
@Document(collection = "user_challenge_progress")
public class UserChallengeProgress {
    @Id
    private String id;

    @Field("challenge_id")
    private String challengeId; // 关联的挑战ID

    @Field("user_id")
    private String userId; // 用户ID

    private String status; // 状态：IN_PROGRESS(进行中), COMPLETED(已完成)

    @Field("joined_at")
    private LocalDateTime joinedAt; // 加入时间

    @Field("completed_at")
    private LocalDateTime completedAt; // 完成时间（可选）

    @Field("reward_claimed")
    private Boolean rewardClaimed; // 是否已领取奖励

    @Field("updated_at")
    private LocalDateTime updatedAt;

    public UserChallengeProgress() {
        this.status = "IN_PROGRESS";
        this.rewardClaimed = false;
        this.joinedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

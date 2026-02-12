package com.example.EcoGo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 挑战实体
 * 环保挑战管理
 *
 * 注意：用户进度从Trip表实时计算，不存储在Challenge中
 * 用户参与记录存储在UserChallengeProgress表中
 */
@Document(collection = "challenges")
public class Challenge {
    @Id
    private String id;
    private String title; // 挑战标题
    private String description; // 挑战描述
    private String type; // 挑战类型：GREEN_TRIPS_DISTANCE(绿色出行总距离,米), CARBON_SAVED(碳排放减少量,克), GREEN_TRIPS_COUNT(绿色出行次数)
    private Double target; // 目标值
    private Integer reward; // 奖励积分
    private String badge; // 徽章ID（完成后获得）
    private String icon; // 图标（emoji）
    private String status; // 状态：ACTIVE(进行中), EXPIRED(已过期)
    private Integer participants; // 参与人数（从UserChallengeProgress计算更新）

    @Field("start_time")
    private LocalDateTime startTime; // 开始时间

    @Field("end_time")
    private LocalDateTime endTime; // 结束时间

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    public Challenge() {
        this.participants = 0;
        this.status = "ACTIVE";
        this.icon = "🏆";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getTarget() {
        return target;
    }

    public void setTarget(Double target) {
        this.target = target;
    }

    public Integer getReward() {
        return reward;
    }

    public void setReward(Integer reward) {
        this.reward = reward;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getParticipants() {
        return participants;
    }

    public void setParticipants(Integer participants) {
        this.participants = participants;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

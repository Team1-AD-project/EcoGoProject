package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "leaderboard_rewards")
public class LeaderboardReward {

    @Id
    private String id;

    @Field("type")
    private String type; // DAILY or MONTHLY

    @Field("period_key")
    private String periodKey; // "2026-02-07" for daily, "2026-02" for monthly

    @Field("user_id")
    private String userId;

    @Field("rank")
    private int rank;

    @Field("points_awarded")
    private long pointsAwarded;

    @Field("carbon_saved")
    private double carbonSaved;

    @Field("distributed_at")
    private LocalDateTime distributedAt;

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public long getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(long pointsAwarded) { this.pointsAwarded = pointsAwarded; }

    public double getCarbonSaved() { return carbonSaved; }
    public void setCarbonSaved(double carbonSaved) { this.carbonSaved = carbonSaved; }

    public LocalDateTime getDistributedAt() { return distributedAt; }
    public void setDistributedAt(LocalDateTime distributedAt) { this.distributedAt = distributedAt; }
}

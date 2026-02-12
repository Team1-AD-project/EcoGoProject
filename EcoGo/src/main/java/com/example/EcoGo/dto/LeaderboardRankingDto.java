package com.example.EcoGo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaderboardRankingDto {

    private String userId;
    private String nickname;
    private int rank;
    private double carbonSaved;
    @JsonProperty("isVip")
    private boolean isVip;
    private long rewardPoints; // 0 if not in top 10

    public LeaderboardRankingDto() {}

    public LeaderboardRankingDto(String userId, String nickname, int rank, double carbonSaved, boolean isVip, String type) {
        this.userId = userId;
        this.nickname = nickname;
        this.rank = rank;
        this.carbonSaved = carbonSaved;
        this.isVip = isVip;
        // Daily: 100→10 pts (×10), Monthly: 1000→100 pts (×100)
        long multiplier = "DAILY".equalsIgnoreCase(type) ? 10L : 100L;
        this.rewardPoints = rank >= 1 && rank <= 10 ? (11 - rank) * multiplier : 0;
    }

    // Getters and Setters

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public double getCarbonSaved() { return carbonSaved; }
    public void setCarbonSaved(double carbonSaved) { this.carbonSaved = carbonSaved; }

    public boolean isVip() { return isVip; }
    public void setVip(boolean vip) { isVip = vip; }

    public long getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(long rewardPoints) { this.rewardPoints = rewardPoints; }
}

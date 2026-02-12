package com.example.EcoGo.dto;

import org.springframework.data.domain.Page;

public class LeaderboardStatsDto {

    private Page<LeaderboardRankingDto> rankingsPage;
    private long totalCarbonSaved;
    private long totalVipUsers;
    private long totalRewardsDistributed;

    public LeaderboardStatsDto(Page<LeaderboardRankingDto> rankingsPage, long totalCarbonSaved,
                               long totalVipUsers, long totalRewardsDistributed) {
        this.rankingsPage = rankingsPage;
        this.totalCarbonSaved = totalCarbonSaved;
        this.totalVipUsers = totalVipUsers;
        this.totalRewardsDistributed = totalRewardsDistributed;
    }

    public Page<LeaderboardRankingDto> getRankingsPage() { return rankingsPage; }
    public void setRankingsPage(Page<LeaderboardRankingDto> rankingsPage) { this.rankingsPage = rankingsPage; }

    public long getTotalCarbonSaved() { return totalCarbonSaved; }
    public void setTotalCarbonSaved(long totalCarbonSaved) { this.totalCarbonSaved = totalCarbonSaved; }

    public long getTotalVipUsers() { return totalVipUsers; }
    public void setTotalVipUsers(long totalVipUsers) { this.totalVipUsers = totalVipUsers; }

    public long getTotalRewardsDistributed() { return totalRewardsDistributed; }
    public void setTotalRewardsDistributed(long totalRewardsDistributed) { this.totalRewardsDistributed = totalRewardsDistributed; }
}

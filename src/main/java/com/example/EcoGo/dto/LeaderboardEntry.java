package com.example.EcoGo.dto;

import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Internal DTO for MongoDB aggregation results.
 * Maps the $group output: { _id: "userId", totalCarbonSaved: sum }
 */
public class LeaderboardEntry {

    @Field("_id")
    private String userId;

    private double totalCarbonSaved;

    public LeaderboardEntry() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getTotalCarbonSaved() { return totalCarbonSaved; }
    public void setTotalCarbonSaved(double totalCarbonSaved) { this.totalCarbonSaved = totalCarbonSaved; }
}

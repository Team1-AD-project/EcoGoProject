package com.example.EcoGo.dto;

public class ChurnRiskDTO {
    private String userId;
    private String riskLevel;

    public ChurnRiskDTO() {}

    public ChurnRiskDTO(String userId, String riskLevel) {
        this.userId = userId;
        this.riskLevel = riskLevel;
    }

    public String getUserId() { return userId; }
    public String getRiskLevel() { return riskLevel; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}

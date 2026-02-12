package com.example.EcoGo.dto;

import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 徽章/服饰购买统计 DTO
 * 用于 MongoDB 聚合 user_badges 集合的结果映射
 * $group by badge_id → _id 就是 badge_id 的值
 */
public class BadgePurchaseStatDto {

    @Field("_id")
    private String badgeId;  // 映射自 $group 的 _id（即 user_badges.badge_id）

    private int purchaseCount;

    public BadgePurchaseStatDto() {
        // Required empty constructor for MongoDB aggregation result deserialization
    }

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(int purchaseCount) {
        this.purchaseCount = purchaseCount;
    }
}

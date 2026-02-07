package com.example.EcoGo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.Map;

/**
 * 徽章/商品 模板表
 */
@Data
@Document(collection = "badges")
public class Badge {

    @Id
    private String id;

    @Field("badge_id")
    private String badgeId;

    private Map<String, String> name;
    private Map<String, String> description;

    @Field("purchase_cost")
    private Integer purchaseCost;

    private String category;

    @Field("sub_category")
    private String subCategory;

    @Field("acquisition_method")
    private String acquisitionMethod; // e.g. "purchase", "achievement", "reward"

    @Field("carbon_threshold")
    private Double carbonThreshold; // 当用户 totalCarbon >= 此值时自动解锁（仅 achievement 类型使用）

    private BadgeIcon icon;

    @Field("is_active")
    private Boolean isActive;

    @Field("created_at")
    private Date createdAt;

    @Data
    public static class BadgeIcon {
        private String url;
        @Field("color_scheme")
        private String colorScheme;
    }
}

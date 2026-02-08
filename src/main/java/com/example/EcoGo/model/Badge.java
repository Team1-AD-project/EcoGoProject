package com.example.EcoGo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


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

    
    private String badgeId;

    private Map<String, String> name;
    private Map<String, String> description;

    
    private Integer purchaseCost;

    private String category;

    
    private String subCategory;

    
    private String acquisitionMethod; // e.g. "purchase", "achievement", "reward"

    
    private Double carbonThreshold; // 当用户 totalCarbon >= 此值时自动解锁（仅 achievement 类型使用）

    private BadgeIcon icon;

    
    private Boolean isActive;

   
    private Date createdAt;

    @Data
    public static class BadgeIcon {
        private String url;
        
        private String colorScheme;
    }
}

package com.example.EcoGo.dto;

import com.example.EcoGo.model.Badge;

import java.util.Map;

public class BadgeDto {

    private String badgeId;
    private Map<String, String> name;
    private Map<String, String> description;
    private Integer purchaseCost;
    private String category;
    private String subCategory;
    private String acquisitionMethod;
    private Double carbonThreshold;
    private BadgeIconDto icon;
    private Boolean isActive;

    public Badge toEntity() {
        Badge badge = new Badge();
        badge.setBadgeId(this.badgeId);
        badge.setName(this.name);
        badge.setDescription(this.description);
        badge.setPurchaseCost(this.purchaseCost);
        badge.setCategory(this.category);
        badge.setSubCategory(this.subCategory);
        badge.setAcquisitionMethod(this.acquisitionMethod);
        badge.setCarbonThreshold(this.carbonThreshold);
        if (this.icon != null) {
            Badge.BadgeIcon badgeIcon = new Badge.BadgeIcon();
            badgeIcon.setUrl(this.icon.getUrl());
            badgeIcon.setColorScheme(this.icon.getColorScheme());
            badge.setIcon(badgeIcon);
        }
        badge.setIsActive(this.isActive);
        return badge;
    }

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    public Integer getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(Integer purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getAcquisitionMethod() {
        return acquisitionMethod;
    }

    public void setAcquisitionMethod(String acquisitionMethod) {
        this.acquisitionMethod = acquisitionMethod;
    }

    public Double getCarbonThreshold() {
        return carbonThreshold;
    }

    public void setCarbonThreshold(Double carbonThreshold) {
        this.carbonThreshold = carbonThreshold;
    }

    public BadgeIconDto getIcon() {
        return icon;
    }

    public void setIcon(BadgeIconDto icon) {
        this.icon = icon;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public static class BadgeIconDto {
        private String url;
        private String colorScheme;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getColorScheme() {
            return colorScheme;
        }

        public void setColorScheme(String colorScheme) {
            this.colorScheme = colorScheme;
        }
    }
}

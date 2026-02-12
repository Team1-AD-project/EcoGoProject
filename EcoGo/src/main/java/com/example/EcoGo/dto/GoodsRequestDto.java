package com.example.EcoGo.dto;

import com.example.EcoGo.model.Goods;

/**
 * DTO for Goods create/update requests.
 * Replaces direct use of the persistent Goods entity in @RequestBody (SonarQube S4684).
 */
public class GoodsRequestDto {
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String category;
    private String type;
    private String brand;
    private String imageUrl;
    private Boolean isActive;
    private Integer vipLevelRequired;
    private Boolean isForRedemption;
    private Integer redemptionPoints;
    private Integer redemptionLimit;

    public Goods toEntity() {
        Goods goods = new Goods();
        goods.setName(this.name);
        goods.setDescription(this.description);
        goods.setPrice(this.price);
        goods.setStock(this.stock);
        goods.setCategory(this.category);
        goods.setType(this.type);
        goods.setBrand(this.brand);
        goods.setImageUrl(this.imageUrl);
        goods.setIsActive(this.isActive);
        goods.setVipLevelRequired(this.vipLevelRequired);
        goods.setIsForRedemption(this.isForRedemption);
        goods.setRedemptionPoints(this.redemptionPoints);
        goods.setRedemptionLimit(this.redemptionLimit);
        return goods;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getVipLevelRequired() { return vipLevelRequired; }
    public void setVipLevelRequired(Integer vipLevelRequired) { this.vipLevelRequired = vipLevelRequired; }

    public Boolean getIsForRedemption() { return isForRedemption; }
    public void setIsForRedemption(Boolean isForRedemption) { this.isForRedemption = isForRedemption; }

    public Integer getRedemptionPoints() { return redemptionPoints; }
    public void setRedemptionPoints(Integer redemptionPoints) { this.redemptionPoints = redemptionPoints; }

    public Integer getRedemptionLimit() { return redemptionLimit; }
    public void setRedemptionLimit(Integer redemptionLimit) { this.redemptionLimit = redemptionLimit; }
}

package com.example.EcoGo.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "advertisements")
public class Advertisement {
    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("status")
    private String status; // Active, Inactive, Paused

    @Field("startDate")
    private LocalDate startDate;

    @Field("endDate")
    private LocalDate endDate;

    @Field("imageUrl")
    private String imageUrl;

    @Field("linkUrl")
    private String linkUrl;

    @Field("position")
    private String position; // banner, sidebar, popup, feed

    @Field("impressions")
    private Long impressions;

    @Field("clicks")
    private Long clicks;

    // Default constructor
    public Advertisement() {
        this.impressions = 0L;
        this.clicks = 0L;
    }

    // Full constructor
    public Advertisement(String name, String description, String status, LocalDate startDate,
                         LocalDate endDate, String imageUrl, String linkUrl, String position) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.position = position;
        this.impressions = 0L;
        this.clicks = 0L;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Long getImpressions() {
        return impressions;
    }

    public void setImpressions(Long impressions) {
        this.impressions = impressions;
    }

    public Long getClicks() {
        return clicks;
    }

    public void setClicks(Long clicks) {
        this.clicks = clicks;
    }

    // Helper method to calculate click rate
    public Double getClickRate() {
        if (impressions == null || impressions == 0) {
            return 0.0;
        }
        return (clicks.doubleValue() / impressions.doubleValue()) * 100;
    }
}

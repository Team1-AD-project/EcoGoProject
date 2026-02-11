package com.example.EcoGo.dto;

import com.example.EcoGo.model.Advertisement;

import java.time.LocalDate;

/**
 * DTO for Advertisement create/update requests.
 * Replaces direct use of the persistent Advertisement entity in @RequestBody (SonarQube S4684).
 */
public class AdvertisementRequestDto {
    private String name;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String imageUrl;
    private String linkUrl;
    private String position;

    public Advertisement toEntity() {
        Advertisement ad = new Advertisement();
        ad.setName(this.name);
        ad.setDescription(this.description);
        ad.setStatus(this.status);
        ad.setStartDate(this.startDate);
        ad.setEndDate(this.endDate);
        ad.setImageUrl(this.imageUrl);
        ad.setLinkUrl(this.linkUrl);
        ad.setPosition(this.position);
        return ad;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
}

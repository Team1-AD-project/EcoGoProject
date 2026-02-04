package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "transport_modes_dict")
public class TransportMode {

    @Id
    private String id; // Manual ID: "1001", etc.

    @Field("mode")
    private String mode; // walk, bike, etc.

    @Field("mode_name")
    private String modeName;

    @Field("carbon_factor")
    private double carbonFactor; // g/km

    @Field("icon")
    private String icon;

    @Field("sort")
    private int sort;

    @Field("is_green")
    private boolean isGreen;

    // Constructors
    public TransportMode() {
    }

    public TransportMode(String id, String mode, String modeName, double carbonFactor, String icon, int sort,
            boolean isGreen) {
        this.id = id;
        this.mode = mode;
        this.modeName = modeName;
        this.carbonFactor = carbonFactor;
        this.icon = icon;
        this.sort = sort;
        this.isGreen = isGreen;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getModeName() {
        return modeName;
    }

    public void setModeName(String modeName) {
        this.modeName = modeName;
    }

    public double getCarbonFactor() {
        return carbonFactor;
    }

    public void setCarbonFactor(double carbonFactor) {
        this.carbonFactor = carbonFactor;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public boolean isGreen() {
        return isGreen;
    }

    public void setGreen(boolean green) {
        isGreen = green;
    }
}

package com.example.EcoGo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PointsDto {

    public static class AdjustPointsRequest {
        public long points; // Amount (+ or -)
        public String source; // admin, trip, etc
        public String description;
        // For admin action details
        public String reason;
    }

    public static class CurrentPointsResponse {
        public String userId;
        public long currentPoints;
        public long totalPoints; // Historical total (only adds, never subtracts)

        public CurrentPointsResponse(String userId, long current, long total) {
            this.userId = userId;
            this.currentPoints = current;
            this.totalPoints = total;
        }
    }

    public static class PointsLogResponse {
        public String id;
        public String change_type;
        public long points;
        public String source;
        public String description; // Actually not in Model but useful? Or maybe mapped from source/reason
        public long balance_after;
        public String created_at;

        // Admin details if any
        public AdminActionDto admin_action;
    }

    public static class AdminActionDto {
        public String operator_id;
        public String reason;
        public String approval_status;
    }

    public static class TripStatsResponse {
        public long totalTrips;
        public long totalPointsEarned;

        public TripStatsResponse(long trips, long points) {
            this.totalTrips = trips;
            this.totalPointsEarned = points;
        }
    }

    // --- Complex Trip Data DTOs ---

    // Generic Request for Settling Points (Trips, Badges, etc.)
    public static class SettleResult {
        public long points;
        public String source; // trip, badge, mission, etc.
        public String description;
        public String relatedId; // TripID, BadgeID, OrderID
    }

    // --- Trip Settlement DTOs ---

    public static class LocationInfo {
        public String address;      // 详细地址
        public String placeName;    // 地点名称
        public String campusZone;   // 区域分类：商务区/校园/商圈

        public LocationInfo() {}

        public LocationInfo(String address, String placeName, String campusZone) {
            this.address = address;
            this.placeName = placeName;
            this.campusZone = campusZone;
        }
    }

    public static class SettleTripRequest {
        public String tripId;               // 行程ID
        public String detectedMode;         // ML识别的主要交通方式
        public double distance;             // 行程总距离(km)
        public long carbonSaved;            // 本次碳减排量(g)
        public boolean isGreenTrip;         // 是否为绿色出行
        public LocationInfo startLocation;  // 起点信息
        public LocationInfo endLocation;    // 终点信息
    }
}

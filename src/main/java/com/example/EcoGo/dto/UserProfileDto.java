package com.example.EcoGo.dto;

import com.example.EcoGo.model.User;
import java.time.LocalDateTime;
import java.util.List;

public class UserProfileDto {

    public static class UpdateProfileRequest {
        public String nickname;
        public String avatar;
        public String phone;
        public String faculty; // New field
        public PreferencesDto preferences;
    }

    public static class PreferencesDto {
        public java.util.List<String> preferredTransport; // Changed to List
        public Boolean enablePush;
        public Boolean enableEmail;
        public Boolean enableBusReminder;
        public String language;
        public String theme;
        public Boolean shareLocation;
        public Boolean showOnLeaderboard;
        public Boolean shareAchievements;

        // New Location Fields
        public String dormitoryOrResidence;
        public String mainTeachingBuilding;
        public String favoriteStudySpot;

        // New Interests & Goals
        public java.util.List<String> interests;
        public Integer weeklyGoals;

        // New Notifications
        public Boolean newChallenges;
        public Boolean activityReminders;
        public Boolean friendActivity;
    }

    public static class AdminManageUserRequest {
        public boolean isAdmin;
        public boolean vip_status;
        public boolean isDeactivated; // New field
        public String remark;
    }

    public static class UserStatusRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("isDeactivated")
        public boolean isDeactivated;
    }

    public static class AdminUpdateUserInfoRequest {
        // userid passed via PathVariable
        public String nickname;
        public String email;
        public Boolean isVipActive; // Replaces isAdmin
        public String vipPlan; // New: e.g. "Monthly", "Yearly"
        public String vipExpiryDate; // New: YYYY-MM-DD or ISO format
        @com.fasterxml.jackson.annotation.JsonProperty("isDeactivated")
        public Boolean isDeactivated;
    }

    public static class UserDetailResponse {
        public User user_info;

        public UserDetailResponse(User user) {
            this.user_info = user;
        }
    }

    public static class UpdateProfileResponse {
        public String user_id;
        public String updated_at;

        public UpdateProfileResponse(String id, LocalDateTime time) {
            this.user_id = id;
            this.updated_at = time.toString();
        }
    }

    public static class AuthCheckResponse {
        public boolean is_admin;
        public List<String> permissions;

        public AuthCheckResponse(boolean isAdmin, List<String> permissions) {
            this.is_admin = isAdmin;
            this.permissions = permissions;
        }
    }

    public static class PreferencesResetResponse {
        public User.Preferences preferences;
        public String updated_at;

        public PreferencesResetResponse(User.Preferences pref, LocalDateTime time) {
            this.preferences = pref;
            this.updated_at = time.toString();
        }
    }
}

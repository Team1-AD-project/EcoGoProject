package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String userid;
    // private String username; // Removed
    private String email;
    private String phone;
    private String password;
    private String nickname;
    private String avatar;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
    @JsonProperty("isDeactivated")
    private boolean isDeactivated;

    private String faculty; // New field

    private Vip vip;
    private Stats stats;
    private Preferences preferences;

    // Core fields
    private long totalCarbon;
    private long totalPoints;
    private long currentPoints;

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ActivityMetrics activityMetrics;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isDeactivated() {
        return isDeactivated;
    }

    public void setDeactivated(boolean deactivated) {
        isDeactivated = deactivated;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public Vip getVip() {
        return vip;
    }

    public void setVip(Vip vip) {
        this.vip = vip;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public long getTotalCarbon() {
        return totalCarbon;
    }

    public void setTotalCarbon(long totalCarbon) {
        this.totalCarbon = totalCarbon;
    }

    public long getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(long totalPoints) {
        this.totalPoints = totalPoints;
    }

    public long getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(long currentPoints) {
        this.currentPoints = currentPoints;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ActivityMetrics getActivityMetrics() {
        return activityMetrics;
    }

    public void setActivityMetrics(ActivityMetrics activityMetrics) {
        this.activityMetrics = activityMetrics;
    }

    // Nested Classes (unchanged)

    public static class Vip {
        private boolean isActive = false;
        private LocalDateTime startDate;
        private LocalDateTime expiryDate;
        private String plan;
        private boolean autoRenew = false;
        private int pointsMultiplier = 1; // Default to 1x for normal users

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
        }

        public LocalDateTime getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(LocalDateTime expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getPlan() {
            return plan;
        }

        public void setPlan(String plan) {
            this.plan = plan;
        }

        public boolean isAutoRenew() {
            return autoRenew;
        }

        public void setAutoRenew(boolean autoRenew) {
            this.autoRenew = autoRenew;
        }

        public int getPointsMultiplier() {
            return pointsMultiplier;
        }

        public void setPointsMultiplier(int pointsMultiplier) {
            this.pointsMultiplier = pointsMultiplier;
        }
    }

    public static class Stats {
        private int totalTrips;
        private double totalDistance;
        private int greenDays;
        private int weeklyRank;
        private int monthlyRank;

        // Cache field for trip-specific points
        private long totalPointsFromTrips;

        public int getTotalTrips() {
            return totalTrips;
        }

        public void setTotalTrips(int totalTrips) {
            this.totalTrips = totalTrips;
        }

        public long getTotalPointsFromTrips() {
            return totalPointsFromTrips;
        }

        public void setTotalPointsFromTrips(long totalPointsFromTrips) {
            this.totalPointsFromTrips = totalPointsFromTrips;
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        public void setTotalDistance(double totalDistance) {
            this.totalDistance = totalDistance;
        }

        public int getGreenDays() {
            return greenDays;
        }

        public void setGreenDays(int greenDays) {
            this.greenDays = greenDays;
        }

        public int getWeeklyRank() {
            return weeklyRank;
        }

        public void setWeeklyRank(int weeklyRank) {
            this.weeklyRank = weeklyRank;
        }

        public int getMonthlyRank() {
            return monthlyRank;
        }

        public void setMonthlyRank(int monthlyRank) {
            this.monthlyRank = monthlyRank;
        }
    }

    public static class Preferences {
        private java.util.List<String> preferredTransport; // Changed to List
        private boolean enablePush;
        private boolean enableEmail;
        private boolean enableBusReminder;
        private String language;
        private String theme;
        private boolean shareLocation;
        private boolean showOnLeaderboard;
        private boolean shareAchievements;

        // New Location Fields
        private String dormitoryOrResidence;
        private String mainTeachingBuilding;
        private String favoriteStudySpot;

        // New Interests & Goals
        private java.util.List<String> interests;
        private int weeklyGoals;

        // New Notifications
        private boolean newChallenges;
        private boolean activityReminders;
        private boolean friendActivity;

        public java.util.List<String> getPreferredTransport() {
            return preferredTransport;
        }

        public void setPreferredTransport(java.util.List<String> preferredTransport) {
            this.preferredTransport = preferredTransport;
        }

        public boolean isEnablePush() {
            return enablePush;
        }

        public void setEnablePush(boolean enablePush) {
            this.enablePush = enablePush;
        }

        public boolean isEnableEmail() {
            return enableEmail;
        }

        public void setEnableEmail(boolean enableEmail) {
            this.enableEmail = enableEmail;
        }

        public boolean isEnableBusReminder() {
            return enableBusReminder;
        }

        public void setEnableBusReminder(boolean enableBusReminder) {
            this.enableBusReminder = enableBusReminder;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public boolean isShareLocation() {
            return shareLocation;
        }

        public void setShareLocation(boolean shareLocation) {
            this.shareLocation = shareLocation;
        }

        public boolean isShowOnLeaderboard() {
            return showOnLeaderboard;
        }

        public void setShowOnLeaderboard(boolean showOnLeaderboard) {
            this.showOnLeaderboard = showOnLeaderboard;
        }

        public boolean isShareAchievements() {
            return shareAchievements;
        }

        public void setShareAchievements(boolean shareAchievements) {
            this.shareAchievements = shareAchievements;
        }

        public String getDormitoryOrResidence() {
            return dormitoryOrResidence;
        }

        public void setDormitoryOrResidence(String dormitoryOrResidence) {
            this.dormitoryOrResidence = dormitoryOrResidence;
        }

        public String getMainTeachingBuilding() {
            return mainTeachingBuilding;
        }

        public void setMainTeachingBuilding(String mainTeachingBuilding) {
            this.mainTeachingBuilding = mainTeachingBuilding;
        }

        public String getFavoriteStudySpot() {
            return favoriteStudySpot;
        }

        public void setFavoriteStudySpot(String favoriteStudySpot) {
            this.favoriteStudySpot = favoriteStudySpot;
        }

        public java.util.List<String> getInterests() {
            return interests;
        }

        public void setInterests(java.util.List<String> interests) {
            this.interests = interests;
        }

        public int getWeeklyGoals() {
            return weeklyGoals;
        }

        public void setWeeklyGoals(int weeklyGoals) {
            this.weeklyGoals = weeklyGoals;
        }

        public boolean isNewChallenges() {
            return newChallenges;
        }

        public void setNewChallenges(boolean newChallenges) {
            this.newChallenges = newChallenges;
        }

        public boolean isActivityReminders() {
            return activityReminders;
        }

        public void setActivityReminders(boolean activityReminders) {
            this.activityReminders = activityReminders;
        }

        public boolean isFriendActivity() {
            return friendActivity;
        }

        public void setFriendActivity(boolean friendActivity) {
            this.friendActivity = friendActivity;
        }
    }

    public static class ActivityMetrics {
        private int activeDays7d;
        private int activeDays30d;
        private int lastTripDays;
        private int loginFrequency7d;
        // Store recent login dates for calculation
        private java.util.List<java.time.LocalDate> loginDates = new java.util.ArrayList<>();

        public int getActiveDays7d() {
            return activeDays7d;
        }

        public void setActiveDays7d(int activeDays7d) {
            this.activeDays7d = activeDays7d;
        }

        public int getActiveDays30d() {
            return activeDays30d;
        }

        public void setActiveDays30d(int activeDays30d) {
            this.activeDays30d = activeDays30d;
        }

        public int getLastTripDays() {
            return lastTripDays;
        }

        public void setLastTripDays(int lastTripDays) {
            this.lastTripDays = lastTripDays;
        }

        public int getLoginFrequency7d() {
            return loginFrequency7d;
        }

        public void setLoginFrequency7d(int loginFrequency7d) {
            this.loginFrequency7d = loginFrequency7d;
        }

        public java.util.List<java.time.LocalDate> getLoginDates() {
            return loginDates;
        }

        public void setLoginDates(java.util.List<java.time.LocalDate> loginDates) {
            this.loginDates = loginDates;
        }
    }
}

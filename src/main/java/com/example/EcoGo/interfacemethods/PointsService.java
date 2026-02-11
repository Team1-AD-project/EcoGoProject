package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.model.UserPointsLog;

import java.util.List;

public interface PointsService {

    /**
     * Adjust user points (Gain or Deduct)
     * 
     * @param userId      The User UUID (or business ID? Need to be careful.
     *                    Typically Service uses UUID internally, but DTO might pass
     *                    Business ID. I'll assume UUID for internal service
     *                    consistency, but will handle resolution if needed)
     *                    Let's stick to UUID for internal service calls to be safe.
     * @param points      Amount (+ or -)
     * @param source      Source of change
     * @param description Brief description
     * @param relatedId   Related ID (Trip/Order/Badge)
     * @param adminAction Optional admin details
     */
    UserPointsLog adjustPoints(String userId, long points, String source, String description, String relatedId,
            UserPointsLog.AdminAction adminAction);

    /**
     * Get current points balance
     */
    PointsDto.CurrentPointsResponse getCurrentPoints(String userId);

    /**
     * Get points history
     */
    List<PointsDto.PointsLogResponse> getPointsHistory(String userId);

    // --- Extended Features ---

    /**
     * Calculate points based on Mode and Distance
     * Formula: (CarCarbon - ModeCarbon) * Distance * 10
     */
    long calculatePoints(String mode, double distance);

    /**
     * Get Trip Statistics
     * 
     * @param userId If null, returns Global stats. If present, returns User stats.
     */
    PointsDto.TripStatsResponse getTripStats(String userId);

    /**
     * Admin: Get all users' current balance (Simple list)
     */
    List<PointsDto.CurrentPointsResponse> getAllUserPoints();

    /**
     * Admin: Get all transaction history
     */
    List<PointsDto.PointsLogResponse> getAllPointsHistory();

    /**
     * Get total points gained by all members of the user's faculty.
     * Calculated from points logs with changeType='gain'.
     */
    // List<UserPointsLog> getHistory(String userId, int page, int size); --
    // existing in implementation

    // Stats
    com.example.EcoGo.dto.FacultyStatsDto.PointsResponse getFacultyTotalPoints(String userId);

    // --- Internal Logic (Not exposed directly as API) ---
    void settle(String userId, PointsDto.SettleResult result);

    void redeemPoints(String userId, String orderId, long points);

    // --- Trip Settlement ---

    // --- Helper Methods for Complex Trips ---

    /**
     * Generate description string: "StartName -> EndName (Distancekm)"
     */
    String formatTripDescription(String startPlace, String endPlace, double totalDistance);

    /**
     * Generate description string from LocationInfo objects
     */
    String formatTripDescription(PointsDto.LocationInfo start, PointsDto.LocationInfo end, double totalDistance);

    /**
     * Generate description string: "Purchased Badge: BadgeName"
     */
    String formatBadgeDescription(String badgeName);
}

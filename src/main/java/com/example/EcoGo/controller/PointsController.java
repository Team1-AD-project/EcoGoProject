package com.example.EcoGo.controller;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.dto.ResponseMessage;
// import com.example.EcoGo.exception.BusinessException; // Removed
// import com.example.EcoGo.exception.errorcode.ErrorCode; // Removed
import com.example.EcoGo.interfacemethods.PointsService;
// import com.example.EcoGo.model.User; // Removed
import com.example.EcoGo.model.UserPointsLog;
// import com.example.EcoGo.repository.UserRepository; // Removed
import com.example.EcoGo.utils.JwtUtils; // Fixed import package name
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PointsController {

    @Autowired
    private PointsService pointsService;

    // UserRepository removed as it's no longer needed in Controller

    @Autowired
    private JwtUtils jwtUtils;

    // --- Mobile Endpoints ---

    /**
     * Get Current Points Balance
     * GET /api/v1/mobile/points/current
     */
    @GetMapping("/api/v1/mobile/points/current")
    public ResponseMessage<PointsDto.CurrentPointsResponse> getCurrentPoints(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtils.getUserIdFromToken(token); // UUID
        return ResponseMessage.success(pointsService.getCurrentPoints(userId));
    }

    /**
     * Get Points History
     * GET /api/v1/mobile/points/history
     */
    @GetMapping("/api/v1/mobile/points/history")
    public ResponseMessage<List<PointsDto.PointsLogResponse>> getHistory(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtils.getUserIdFromToken(token); // UUID
        return ResponseMessage.success(pointsService.getPointsHistory(userId));
    }

    /**
     * Mobile: Calculate Points (Estimator)
     * GET /api/v1/mobile/points/calculate?mode=walk&distance=5
     */
    @GetMapping("/api/v1/mobile/points/calculate")
    public ResponseMessage<Long> calculatePoints(@RequestParam String mode, @RequestParam double distance) {
        return ResponseMessage.success(pointsService.calculatePoints(mode, distance));
    }

    /**
     * Mobile: Get Trip Stats
     * GET /api/v1/mobile/points/stats/trip
     */
    @GetMapping("/api/v1/mobile/points/stats/trip")
    public ResponseMessage<PointsDto.TripStatsResponse> getMyTripStats(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtils.getUserIdFromToken(token);
        return ResponseMessage.success(pointsService.getTripStats(userId));
    }

    /**
     * Mobile: Get Total Faculty Points (Calculated from Logs)
     * GET /api/v1/mobile/points/stats/faculty
     */
    @GetMapping("/api/v1/mobile/points/stats/faculty")
    public ResponseMessage<Long> getFacultyPoints(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtils.getUserIdFromToken(token);
        return ResponseMessage.success(pointsService.getFacultyTotalPoints(userId));
    }

    // --- Web / Admin Endpoints ---

    /**
     * Admin: List All Users Points (Balance)
     * GET /api/v1/web/points/all
     */
    @GetMapping("/api/v1/web/points/all")
    public ResponseMessage<List<PointsDto.CurrentPointsResponse>> getAllUserPoints() {
        return ResponseMessage.success(pointsService.getAllUserPoints());
    }

    /**
     * Admin: List All Transactions
     * GET /api/v1/web/points/history/all
     */
    @GetMapping("/api/v1/web/points/history/all")
    public ResponseMessage<List<PointsDto.PointsLogResponse>> getAllPointsHistory() {
        return ResponseMessage.success(pointsService.getAllPointsHistory());
    }

    /**
     * Admin: Global Trip Stats
     * GET /api/v1/web/points/stats/trip/all
     */
    @GetMapping("/api/v1/web/points/stats/trip/all")
    public ResponseMessage<PointsDto.TripStatsResponse> getGlobalTripStats() {
        return ResponseMessage.success(pointsService.getTripStats(null));
    }

    /**
     * Admin: Specific User Trip Stats
     * GET /api/v1/web/users/{userid}/points/stats/trip
     */
    @GetMapping("/api/v1/web/users/{userid}/points/stats/trip")
    public ResponseMessage<PointsDto.TripStatsResponse> getUserTripStats(@PathVariable String userid) {
        // Pass Business ID directly to Service
        return ResponseMessage.success(pointsService.getTripStats(userid));
    }

    /**
     * Admin: Get User Current Points
     * GET /api/v1/web/points/user/{userid}/current
     */
    @GetMapping("/api/v1/web/points/user/{userid}/current")
    public ResponseMessage<PointsDto.CurrentPointsResponse> getAdminUserBalance(@PathVariable String userid) {
        // Pass Business ID directly to Service
        return ResponseMessage.success(pointsService.getCurrentPoints(userid));
    }

    /**
     * Admin: Get User Points History
     * GET /api/v1/web/points/user/{userid}/history
     */
    @GetMapping("/api/v1/web/points/user/{userid}/history")
    public ResponseMessage<List<PointsDto.PointsLogResponse>> getAdminUserHistory(@PathVariable String userid) {
        // Pass Business ID directly to Service
        return ResponseMessage.success(pointsService.getPointsHistory(userid));
    }

    /**
     * Admin Adjust Points
     * POST /api/v1/web/users/{userid}/points/adjust
     * Body: { "points": 100, "source": "admin", "description": "Compensation",
     * "reason": "System Error" }
     */
    @PostMapping("/api/v1/web/users/{userid}/points/adjust")
    public ResponseMessage<String> adjustPointsAdmin(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String userid,
            @RequestBody PointsDto.AdjustPointsRequest request) {

        // 1. Prepare Admin Action Info
        UserPointsLog.AdminAction adminAction = new UserPointsLog.AdminAction();
        // Extract operator ID from token (Now it IS Business ID)
        String token = authHeader.replace("Bearer ", "");
        String operatorId = jwtUtils.getUserIdFromToken(token);

        adminAction.setOperatorId(operatorId);
        adminAction.setReason(request.reason);
        adminAction.setApprovalStatus("approved");

        // 2. Call Service (Pass Business ID directly)
        String description = request.description != null ? request.description : request.reason;

        pointsService.adjustPoints(userid, request.points, request.source, description, null, adminAction);

        return ResponseMessage.success("Points adjusted successfully");
    }
}

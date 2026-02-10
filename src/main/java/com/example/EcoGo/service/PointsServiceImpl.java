package com.example.EcoGo.service;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserPointsLog;
import com.example.EcoGo.repository.UserPointsLogRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.interfacemethods.BadgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PointsServiceImpl implements PointsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointsLogRepository pointsLogRepository;

    @Autowired
    @Lazy
    private BadgeService badgeService;

    @Override
    public UserPointsLog adjustPoints(String userId, long points, String source, String description, String relatedId,
            UserPointsLog.AdminAction adminAction) {
        // 1. Fetch User (using UUID)
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. Validate sufficient funds for deduction
        long newBalance = user.getCurrentPoints() + points;
        if (newBalance < 0) {
            // Usually we don't allow negative balance
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Insufficient points");
        }

        // 3. Update User
        user.setCurrentPoints(newBalance);
        // Logic Refinement:
        // - "trip": Add to Total (Lifetime) + Current.
        // - "badges"/"redeem" (Refunds): Only Current.
        // - "badges" (Purchase): Subtract Current (handled by points < 0 check).

        // Prevent infinite rank exploit via Buy/Refund cycles.
        // Only valid "earning" sources increase Total Points.
        boolean isEarningSource = "trip".equalsIgnoreCase(source)
                || "mission".equalsIgnoreCase(source)
                || "task".equalsIgnoreCase(source)
                || "admin".equalsIgnoreCase(source)
                || "leaderboard".equalsIgnoreCase(source)
                || "challenges".equalsIgnoreCase(source);

        if (points > 0 && isEarningSource) {
            user.setTotalPoints(user.getTotalPoints() + points);
        }

        // 累计碳减排量（trip 来源时，points / 10 = 碳减排克数）
        boolean isTripSource = "trip".equalsIgnoreCase(source);
        if (points > 0 && isTripSource) {
            double carbonSaved = points / 10.0;
            double newTotal = user.getTotalCarbon() + carbonSaved;
            user.setTotalCarbon(Math.round(newTotal * 100.0) / 100.0);
        }

        userRepository.save(user);

        // 检查是否有碳减排成就徽章可以自动解锁
        if (isTripSource && points > 0) {
            badgeService.checkAndUnlockCarbonBadges(userId);
        }

        // 4. Create Log
        String changeType = points > 0 ? "gain" : (points < 0 ? "deduct" : "info");
        // If source is REDEEM, type might be redeem
        if ("redeem".equalsIgnoreCase(source)) {
            changeType = "redeem";
        }

        UserPointsLog log = new UserPointsLog();
        log.setId(java.util.UUID.randomUUID().toString()); // Use UUID for Log ID
        log.setUserId(user.getUserid()); // Store Business UserID (e.g. "user001") instead of UUID
        log.setChangeType(changeType);
        log.setPoints(points);
        log.setSource(source);
        log.setDescription(description);
        log.setRelatedId(relatedId);
        log.setAdminAction(adminAction);
        log.setBalanceAfter(newBalance);

        return pointsLogRepository.save(log);
    }

    @Override
    public PointsDto.CurrentPointsResponse getCurrentPoints(String userId) {
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new PointsDto.CurrentPointsResponse(user.getUserid(), user.getCurrentPoints(), user.getTotalPoints());
    }

    @Autowired
    private com.example.EcoGo.repository.TransportModeRepository transportModeRepository;

    @Override
    public long calculatePoints(String mode, double distance) {
        // 1. Fetch Mode
        var transportMode = transportModeRepository.findByMode(mode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "Invalid transport mode: " + mode));

        // 2. Constants (Should match DB)
        double carCarbon = 100.0; // g/km (Benchmark)

        // 3. Calc Saving: (Car - Current) * Distance
        double savingPerKm = carCarbon - transportMode.getCarbonFactor();
        if (savingPerKm < 0)
            savingPerKm = 0; // Should not punish? Or maybe negative points? Assuming non-negative reward.

        double totalCarbonSaved = savingPerKm * distance;

        // 4. Points: 1g = 10 pts
        return (long) (totalCarbonSaved * 10);
    }

    @Override
    public PointsDto.TripStatsResponse getTripStats(String userId) {
        if (userId == null) {
            // Global stats - Aggregate logs (because scanning all users is expensive too,
            // and logs source='trip' is accurate)
            List<UserPointsLog> logs = pointsLogRepository.findBySource("trip");
            long totalTrips = logs.size();
            long totalPoints = logs.stream().mapToLong(UserPointsLog::getPoints).sum();
            return new PointsDto.TripStatsResponse(totalTrips, totalPoints);
        } else {
            // User stats - Read from User.Stats Cache (Fast)
            User user = userRepository.findByUserid(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            User.Stats stats = user.getStats();
            long totalTrips = 0;
            long totalPoints = 0;

            if (stats != null) {
                totalTrips = stats.getTotalTrips();
                totalPoints = stats.getTotalPointsFromTrips();
            }

            return new PointsDto.TripStatsResponse(totalTrips, totalPoints);
        }
    }

    @Override
    public List<PointsDto.CurrentPointsResponse> getAllUserPoints() {
        return userRepository.findAll().stream()
                .map(user -> new PointsDto.CurrentPointsResponse(user.getUserid(), user.getCurrentPoints(),
                        user.getTotalPoints()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PointsDto.PointsLogResponse> getAllPointsHistory() {
        return pointsLogRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void settle(String userId, PointsDto.SettleResult result) {
        // Logic simplified: Caller calculates points and description
        long points = result.points;
        String source = result.source != null ? result.source : "general";
        String description = result.description != null ? result.description : "Points adjustment";
        String relatedId = result.relatedId;

        // Reuse adjustPoints logic (Handles log and balance)
        adjustPoints(userId, points, source, description, relatedId, null);
    }

    @Override
    public void redeemPoints(String userId, String orderId, long points) {
        String description = "Redemption for order: " + orderId;
        // Points should be negative for deduction
        adjustPoints(userId, -Math.abs(points), "redeem", description, orderId, null);
    }

    // Helper to avoid duplication
    private PointsDto.PointsLogResponse convertToDto(UserPointsLog log) {
        PointsDto.PointsLogResponse dto = new PointsDto.PointsLogResponse();
        dto.id = log.getId();
        dto.change_type = log.getChangeType();
        dto.points = log.getPoints();
        dto.source = log.getSource();
        dto.balance_after = log.getBalanceAfter();
        dto.created_at = log.getCreatedAt().toString();

        if (log.getAdminAction() != null) {
            PointsDto.AdminActionDto adminDto = new PointsDto.AdminActionDto();
            adminDto.operator_id = log.getAdminAction().getOperatorId();
            adminDto.reason = log.getAdminAction().getReason();
            adminDto.approval_status = log.getAdminAction().getApprovalStatus();
            dto.admin_action = adminDto;
        }
        return dto;
    }

    @Override
    public List<PointsDto.PointsLogResponse> getPointsHistory(String userId) {
        List<UserPointsLog> logs = pointsLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return logs.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public String formatTripDescription(String startPlace, String endPlace, double totalDistance) {
        String startName = (startPlace != null && !startPlace.isEmpty()) ? startPlace : "Unknown Start";
        String endName = (endPlace != null && !endPlace.isEmpty()) ? endPlace : "Unknown Destination";

        // Format: "Start -> End (2.5km)"
        // Using String.format for cleaner output
        return String.format("%s -> %s (%.1fkm)", startName, endName, totalDistance);
    }

    @Override
    public void settleTrip(String userId, PointsDto.SettleTripRequest request) {
        // 1. Calculate points based on detected mode and distance
        long points = calculatePoints(request.detectedMode, request.distance);

        // 2. Generate description from LocationInfo
        String description = formatTripDescription(request.startLocation, request.endLocation, request.distance);

        // 3. Adjust points (handles balance, totalPoints, totalCarbon, badge check,
        // log)
        adjustPoints(userId, points, "trip", description, request.tripId, null);

        // 4. Update User.Stats cache (REMOVED: User.Stats is deprecated/unused object)
    }

    @Override
    public String formatTripDescription(PointsDto.LocationInfo start, PointsDto.LocationInfo end,
            double totalDistance) {
        String startName = (start != null && start.placeName != null && !start.placeName.isEmpty())
                ? start.placeName
                : "Unknown Start";
        String endName = (end != null && end.placeName != null && !end.placeName.isEmpty())
                ? end.placeName
                : "Unknown Destination";
        return String.format("%s -> %s (%.1fkm)", startName, endName, totalDistance);
    }

    @Override
    public String formatBadgeDescription(String badgeName) {
        // Format: "Purchased Badge: Eco Pioneer"
        return "Purchased Badge: " + (badgeName != null ? badgeName : "Unknown Badge");
    }

    @Override
    public com.example.EcoGo.dto.FacultyStatsDto.PointsResponse getFacultyTotalPoints(String userId) {
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String faculty = user.getFaculty();
        // If faculty is null/empty, we can return 0 or empty string
        if (faculty == null || faculty.isEmpty()) {
            return new com.example.EcoGo.dto.FacultyStatsDto.PointsResponse("", 0L);
        }

        List<User> facultyUsers = userRepository.findByFaculty(faculty);
        long totalPoints = facultyUsers.stream()
                .mapToLong(User::getTotalPoints)
                .sum();

        return new com.example.EcoGo.dto.FacultyStatsDto.PointsResponse(faculty, totalPoints);
    }
}

package com.example.EcoGo.service;

import com.example.EcoGo.dto.AnalyticsSummaryDto;
import com.example.EcoGo.interfacemethods.StatisticsInterface;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.utils.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsImplementation implements StatisticsInterface {

    private static final Logger log = LoggerFactory.getLogger(StatisticsImplementation.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Override
    public AnalyticsSummaryDto getManagementAnalytics(String timeRange) {
        log.info("[getManagementAnalytics] Called with timeRange={}", LogSanitizer.sanitize(timeRange));

        List<User> allUsers = safeLoadUsers();
        if (allUsers == null) {
            return emptyAnalyticsSummary();
        }

        log.info("[getManagementAnalytics] Loaded {} users", allUsers.size());
        List<User> nonAdminUsers = allUsers.stream()
                .filter(u -> !u.isAdmin())
                .collect(Collectors.toList());

        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();

        TrendResult trend = buildTrends(timeRange, nonAdminUsers);
        summary.setUserGrowthTrend(trend.userTrend());
        summary.setCarbonGrowthTrend(trend.carbonTrend());

        applyMetricCardsFromTrend(summary, trend.userTrend(), trend.carbonTrend());
        applyVipDistribution(summary, nonAdminUsers);
        applyRevenuePlaceholders(summary);

        return summary;
    }

    // =========================
    // 1) Safe load + empty DTO
    // =========================
    private List<User> safeLoadUsers() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            log.error("[getManagementAnalytics] Failed to load users: {}", e.getMessage(), e);
            return null;
        }
    }

    private AnalyticsSummaryDto emptyAnalyticsSummary() {
        AnalyticsSummaryDto empty = new AnalyticsSummaryDto();
        empty.setTotalUsers(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setNewUsers(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setActiveUsers(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setTotalRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setVipRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setShopRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        empty.setUserGrowthTrend(new ArrayList<>());
        empty.setCarbonGrowthTrend(new ArrayList<>());
        empty.setRevenueGrowthTrend(new ArrayList<>());
        empty.setVipDistribution(new ArrayList<>());
        empty.setCategoryRevenue(new ArrayList<>());
        return empty;
    }

    // =========================
    // 2) Trend building
    // =========================
    private record TrendResult(
            List<AnalyticsSummaryDto.UserGrowthPoint> userTrend,
            List<AnalyticsSummaryDto.CarbonGrowthPoint> carbonTrend
    ) {}

    private TrendResult buildTrends(String timeRange, List<User> nonAdminUsers) {
        List<AnalyticsSummaryDto.UserGrowthPoint> userTrend = new ArrayList<>();
        List<AnalyticsSummaryDto.CarbonGrowthPoint> carbonTrend = new ArrayList<>();

        boolean weekly = "weekly".equalsIgnoreCase(timeRange);
        if (weekly) {
            buildWeeklyTrend(nonAdminUsers, userTrend, carbonTrend);
        } else {
            buildMonthlyTrend(nonAdminUsers, userTrend, carbonTrend);
        }

        Collections.reverse(userTrend);
        Collections.reverse(carbonTrend);
        return new TrendResult(userTrend, carbonTrend);
    }

    private void buildWeeklyTrend(
            List<User> nonAdminUsers,
            List<AnalyticsSummaryDto.UserGrowthPoint> userTrend,
            List<AnalyticsSummaryDto.CarbonGrowthPoint> carbonTrend
    ) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (int i = 0; i < 5; i++) {
            LocalDate weekDate = today.minusWeeks(i);
            int weekNumber = weekDate.get(weekFields.weekOfWeekBasedYear());

            LocalDateTime weekEnd = weekDate.with(weekFields.dayOfWeek(), 7).atTime(23, 59, 59);
            LocalDateTime weekStart = weekEnd.minusWeeks(1).plusNanos(1);

            PeriodUserStats stats = computeUserStatsForPeriod(nonAdminUsers, weekStart, weekEnd, true);
            CarbonStats carbon = computeCarbonStatsForPeriod(weekStart, weekEnd);

            String label = "W" + weekNumber;
            userTrend.add(new AnalyticsSummaryDto.UserGrowthPoint(
                    label,
                    stats.totalUsersAtPeriodEnd(),
                    stats.newUsersInPeriod(),
                    stats.activeUsersInPeriod()
            ));
            carbonTrend.add(new AnalyticsSummaryDto.CarbonGrowthPoint(
                    label,
                    carbon.carbonSaved(),
                    stats.activeUsersInPeriod() > 0 ? (double) carbon.carbonSaved() / stats.activeUsersInPeriod() : 0
            ));
        }
    }

    private void buildMonthlyTrend(
            List<User> nonAdminUsers,
            List<AnalyticsSummaryDto.UserGrowthPoint> userTrend,
            List<AnalyticsSummaryDto.CarbonGrowthPoint> carbonTrend
    ) {
        YearMonth currentMonth = YearMonth.now();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM");

        for (int i = 0; i < 6; i++) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime monthStart = month.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = month.atEndOfMonth().atTime(23, 59, 59);

            PeriodUserStats stats = computeUserStatsForPeriod(nonAdminUsers, monthStart, monthEnd, false);
            CarbonStats carbon = computeCarbonStatsForPeriod(monthStart, monthEnd);

            String label = month.format(monthFmt);
            userTrend.add(new AnalyticsSummaryDto.UserGrowthPoint(
                    label,
                    stats.totalUsersAtPeriodEnd(),
                    stats.newUsersInPeriod(),
                    stats.activeUsersInPeriod()
            ));
            carbonTrend.add(new AnalyticsSummaryDto.CarbonGrowthPoint(
                    label,
                    carbon.carbonSaved(),
                    stats.activeUsersInPeriod() > 0 ? (double) carbon.carbonSaved() / stats.activeUsersInPeriod() : 0
            ));
        }
    }

    private record PeriodUserStats(long totalUsersAtPeriodEnd, long newUsersInPeriod, long activeUsersInPeriod) {}

    private PeriodUserStats computeUserStatsForPeriod(
            List<User> nonAdminUsers,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            boolean weekly
    ) {
        long totalUsersAtEnd = nonAdminUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isBefore(periodEnd))
                .count();

        long newUsers = nonAdminUsers.stream()
                .filter(u -> u.getCreatedAt() != null
                        && u.getCreatedAt().isAfter(periodStart)
                        && u.getCreatedAt().isBefore(periodEnd))
                .count();

        long activeUsers = nonAdminUsers.stream()
                .filter(u -> isActiveInPeriod(u, periodStart, weekly))
                .count();

        return new PeriodUserStats(totalUsersAtEnd, newUsers, activeUsers);
    }

    private boolean isActiveInPeriod(User u, LocalDateTime periodStart, boolean weekly) {
        if (u.getActivityMetrics() == null) return false;
        if (u.getUpdatedAt() == null) return false;
        if (!u.getUpdatedAt().isAfter(periodStart)) return false;

        if (weekly) {
            return u.getActivityMetrics().getActiveDays7d() > 0;
        }
        return u.getActivityMetrics().getActiveDays30d() > 0;
    }

    private record CarbonStats(long carbonSaved) {}

    private CarbonStats computeCarbonStatsForPeriod(LocalDateTime periodStart, LocalDateTime periodEnd) {
        List<Trip> trips = tripRepository.findByStartTimeBetweenAndCarbonStatus(periodStart, periodEnd, "completed");
        long carbonSaved = Math.round(trips.stream().mapToDouble(Trip::getCarbonSaved).sum());
        return new CarbonStats(carbonSaved);
    }

    // =========================
    // 3) Metric cards + VIP + Revenue placeholders
    // =========================
    private void applyMetricCardsFromTrend(
            AnalyticsSummaryDto summary,
            List<AnalyticsSummaryDto.UserGrowthPoint> userTrend,
            List<AnalyticsSummaryDto.CarbonGrowthPoint> carbonTrend
    ) {
        if (userTrend.size() >= 2 && carbonTrend.size() >= 2) {
            AnalyticsSummaryDto.UserGrowthPoint latestUser = userTrend.get(userTrend.size() - 1);
            AnalyticsSummaryDto.UserGrowthPoint previousUser = userTrend.get(userTrend.size() - 2);
            AnalyticsSummaryDto.CarbonGrowthPoint latestCarbon = carbonTrend.get(carbonTrend.size() - 1);
            AnalyticsSummaryDto.CarbonGrowthPoint previousCarbon = carbonTrend.get(carbonTrend.size() - 2);

            summary.setTotalUsers(new AnalyticsSummaryDto.Metric((double) latestUser.getUsers(), (double) previousUser.getUsers()));
            summary.setNewUsers(new AnalyticsSummaryDto.Metric((double) latestUser.getNewUsers(), (double) previousUser.getNewUsers()));
            summary.setActiveUsers(new AnalyticsSummaryDto.Metric((double) latestUser.getActiveUsers(), (double) previousUser.getActiveUsers()));
            summary.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric((double) latestCarbon.getCarbonSaved(), (double) previousCarbon.getCarbonSaved()));
            summary.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(latestCarbon.getAvgPerUser(), previousCarbon.getAvgPerUser()));
            return;
        }

        summary.setTotalUsers(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setNewUsers(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setActiveUsers(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(0, 0));
    }

    private void applyVipDistribution(AnalyticsSummaryDto summary, List<User> nonAdminUsers) {
        long vipActive = nonAdminUsers.stream()
                .filter(u -> u.getVip() != null && u.getVip().isActive())
                .count();
        long vipInactive = nonAdminUsers.size() - vipActive;

        summary.setVipDistribution(Arrays.asList(
                new AnalyticsSummaryDto.DistributionPoint("VIP Active", vipActive),
                new AnalyticsSummaryDto.DistributionPoint("Non-VIP", vipInactive)
        ));
    }

    private void applyRevenuePlaceholders(AnalyticsSummaryDto summary) {
        // Revenue (placeholder — points-based)
        summary.setTotalRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setVipRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setShopRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setCategoryRevenue(new ArrayList<>());
        summary.setRevenueGrowthTrend(new ArrayList<>());
    }

    @Override
    public Long getRedemptionVolume() {
        return 0L;
    }
}

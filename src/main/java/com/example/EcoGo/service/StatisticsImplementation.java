package com.example.EcoGo.service;

import com.example.EcoGo.dto.AnalyticsSummaryDto;
import com.example.EcoGo.interfacemethods.StatisticsInterface;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        log.info("[getManagementAnalytics] Called with timeRange={}", timeRange);
        List<User> allUsers;
        try {
            allUsers = userRepository.findAll();
        } catch (Exception e) {
            log.error("[getManagementAnalytics] Failed to load users: {}", e.getMessage(), e);
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
        log.info("[getManagementAnalytics] Loaded {} users", allUsers.size());
        List<User> nonAdminUsers = allUsers.stream().filter(u -> !u.isAdmin()).collect(Collectors.toList());

        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();
        List<AnalyticsSummaryDto.UserGrowthPoint> userTrend = new ArrayList<>();
        List<AnalyticsSummaryDto.CarbonGrowthPoint> carbonTrend = new ArrayList<>();

        if ("weekly".equals(timeRange)) {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 5; i++) {
                LocalDate weekDate = today.minusWeeks(i);
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                int weekNumber = weekDate.get(weekFields.weekOfWeekBasedYear());

                LocalDateTime weekEnd = weekDate.with(weekFields.dayOfWeek(), 7).atTime(23, 59, 59);
                LocalDateTime weekStart = weekEnd.minusWeeks(1).plusNanos(1);

                long totalUsersAtEndOfWeek = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isBefore(weekEnd)).count();
                long newUsersInWeek = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(weekStart) && u.getCreatedAt().isBefore(weekEnd)).count();
                long activeUsersInWeek = nonAdminUsers.stream().filter(u -> u.getActivityMetrics() != null && u.getActivityMetrics().getActiveDays7d() > 0 && u.getUpdatedAt() != null && u.getUpdatedAt().isAfter(weekStart)).count();

                // Sum carbon_saved from completed trips in this week
                List<Trip> weekTrips = tripRepository.findByStartTimeBetweenAndCarbonStatus(weekStart, weekEnd, "completed");
                long carbonSavedInWeek = Math.round(weekTrips.stream().mapToDouble(Trip::getCarbonSaved).sum());

                userTrend.add(new AnalyticsSummaryDto.UserGrowthPoint("W" + weekNumber, totalUsersAtEndOfWeek, newUsersInWeek, activeUsersInWeek));
                carbonTrend.add(new AnalyticsSummaryDto.CarbonGrowthPoint("W" + weekNumber, carbonSavedInWeek, activeUsersInWeek > 0 ? (double)carbonSavedInWeek/activeUsersInWeek : 0));
            }
        } else { // monthly
            YearMonth currentMonth = YearMonth.now();
            for (int i = 0; i < 6; i++) {
                YearMonth month = currentMonth.minusMonths(i);
                LocalDateTime monthEnd = month.atEndOfMonth().atTime(23, 59, 59);
                LocalDateTime monthStart = month.atDay(1).atStartOfDay();

                long totalUsersAtEndOfMonth = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isBefore(monthEnd)).count();
                long newUsersInMonth = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart) && u.getCreatedAt().isBefore(monthEnd)).count();
                long activeUsersInMonth = nonAdminUsers.stream().filter(u -> u.getActivityMetrics() != null && u.getActivityMetrics().getActiveDays30d() > 0 && u.getUpdatedAt() != null && u.getUpdatedAt().isAfter(monthStart)).count();

                // Sum carbon_saved from completed trips in this month
                List<Trip> monthTrips = tripRepository.findByStartTimeBetweenAndCarbonStatus(monthStart, monthEnd, "completed");
                long carbonSavedInMonth = Math.round(monthTrips.stream().mapToDouble(Trip::getCarbonSaved).sum());

                userTrend.add(new AnalyticsSummaryDto.UserGrowthPoint(month.format(DateTimeFormatter.ofPattern("MMM")), totalUsersAtEndOfMonth, newUsersInMonth, activeUsersInMonth));
                carbonTrend.add(new AnalyticsSummaryDto.CarbonGrowthPoint(month.format(DateTimeFormatter.ofPattern("MMM")), carbonSavedInMonth, activeUsersInMonth > 0 ? (double)carbonSavedInMonth/activeUsersInMonth : 0));
            }
        }

        Collections.reverse(userTrend);
        Collections.reverse(carbonTrend);

        summary.setUserGrowthTrend(userTrend);
        summary.setCarbonGrowthTrend(carbonTrend);

        // Set Metric Cards from the latest two data points
        if (userTrend.size() >= 2 && carbonTrend.size() >= 2) {
            AnalyticsSummaryDto.UserGrowthPoint latestUser = userTrend.get(userTrend.size() - 1);
            AnalyticsSummaryDto.UserGrowthPoint previousUser = userTrend.get(userTrend.size() - 2);
            AnalyticsSummaryDto.CarbonGrowthPoint latestCarbon = carbonTrend.get(carbonTrend.size() - 1);
            AnalyticsSummaryDto.CarbonGrowthPoint previousCarbon = carbonTrend.get(carbonTrend.size() - 2);

            summary.setTotalUsers(new AnalyticsSummaryDto.Metric((double)latestUser.getUsers(), (double)previousUser.getUsers()));
            summary.setNewUsers(new AnalyticsSummaryDto.Metric((double)latestUser.getNewUsers(), (double)previousUser.getNewUsers()));
            summary.setActiveUsers(new AnalyticsSummaryDto.Metric((double)latestUser.getActiveUsers(), (double)previousUser.getActiveUsers()));
            summary.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric((double)latestCarbon.getCarbonSaved(), (double)previousCarbon.getCarbonSaved()));
            summary.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(latestCarbon.getAvgPerUser(), previousCarbon.getAvgPerUser()));
        } else {
            summary.setTotalUsers(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setNewUsers(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setActiveUsers(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(0, 0));
        }

        // --- VIP Distribution (Real Data) ---
        long vipActive = nonAdminUsers.stream().filter(u -> u.getVip() != null && u.getVip().isActive()).count();
        long vipInactive = nonAdminUsers.size() - vipActive;
        summary.setVipDistribution(Arrays.asList(
                new AnalyticsSummaryDto.DistributionPoint("VIP Active", vipActive),
                new AnalyticsSummaryDto.DistributionPoint("Non-VIP", vipInactive)
        ));

        // --- Revenue (placeholder — points-based) ---
        summary.setTotalRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setVipRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setShopRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        summary.setCategoryRevenue(new ArrayList<>());
        summary.setRevenueGrowthTrend(new ArrayList<>());

        return summary;
    }

    @Override
    public Long getRedemptionVolume() {
        return 0L;
    }
}

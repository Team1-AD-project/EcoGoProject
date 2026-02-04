package com.example.EcoGo.service;

import com.example.EcoGo.dto.AnalyticsSummaryDto;
import com.example.EcoGo.interfacemethods.StatisticsInterface;
import com.example.EcoGo.model.Ranking;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserPointsLog;
import com.example.EcoGo.repository.RankingRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.repository.UserPointsLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class StatisticsImplementation implements StatisticsInterface {

    @Autowired
    private UserPointsLogRepository pointsLogRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RankingRepository rankingRepository;

    @Override
    public AnalyticsSummaryDto getManagementAnalytics(String timeRange) {
        List<User> allUsers = userRepository.findAll();
        List<User> nonAdminUsers = allUsers.stream().filter(u -> !u.isAdmin()).collect(Collectors.toList());
        Map<String, List<Ranking>> rankingsByPeriod = rankingRepository.findAll().stream()
                .filter(r -> r.getPeriod() != null)
                .collect(Collectors.groupingBy(Ranking::getPeriod));

        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();
        List<AnalyticsSummaryDto.UserGrowthPoint> userTrend = new ArrayList<>();
        List<AnalyticsSummaryDto.CarbonGrowthPoint> carbonTrend = new ArrayList<>();

        if ("weekly".equals(timeRange)) {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 5; i++) { // Last 5 weeks
                LocalDate weekDate = today.minusWeeks(i);
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                int weekNumber = weekDate.get(weekFields.weekOfWeekBasedYear());
                int year = weekDate.getYear();
                String periodName = "Week " + weekNumber + ", " + year;

                LocalDateTime weekEnd = weekDate.with(weekFields.dayOfWeek(), 7).atTime(23, 59, 59);
                LocalDateTime weekStart = weekEnd.minusWeeks(1).plusNanos(1);

                long totalUsersAtEndOfWeek = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isBefore(weekEnd)).count();
                long newUsersInWeek = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(weekStart) && u.getCreatedAt().isBefore(weekEnd)).count();
                long activeUsersInWeek = nonAdminUsers.stream().filter(u -> u.getActivityMetrics() != null && u.getActivityMetrics().getActiveDays7d() > 0 && u.getUpdatedAt().isAfter(weekStart)).count();
                
                long carbonSavedInWeek = rankingsByPeriod.getOrDefault(periodName, Collections.emptyList()).stream().mapToLong(Ranking::getCarbonSaved).sum();

                userTrend.add(new AnalyticsSummaryDto.UserGrowthPoint("W" + weekNumber, totalUsersAtEndOfWeek, newUsersInWeek, activeUsersInWeek));
                carbonTrend.add(new AnalyticsSummaryDto.CarbonGrowthPoint("W" + weekNumber, carbonSavedInWeek, activeUsersInWeek > 0 ? (double)carbonSavedInWeek/activeUsersInWeek : 0));
            }
        } else { // monthly
            YearMonth currentMonth = YearMonth.now();
            for (int i = 0; i < 6; i++) { // Last 6 months
                YearMonth month = currentMonth.minusMonths(i);
                LocalDateTime monthEnd = month.atEndOfMonth().atTime(23, 59, 59);
                LocalDateTime monthStart = month.atDay(1).atStartOfDay();

                long totalUsersAtEndOfMonth = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isBefore(monthEnd)).count();
                long newUsersInMonth = nonAdminUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart) && u.getCreatedAt().isBefore(monthEnd)).count();
                long activeUsersInMonth = nonAdminUsers.stream().filter(u -> u.getActivityMetrics() != null && u.getActivityMetrics().getActiveDays30d() > 0 && u.getUpdatedAt().isAfter(monthStart)).count();
                
                final YearMonth finalMonth = month;
                long carbonSavedInMonth = rankingsByPeriod.values().stream().flatMap(List::stream)
                    .filter(r -> r.getStartDate() != null && YearMonth.from(r.getStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()).equals(finalMonth))
                    .mapToLong(Ranking::getCarbonSaved).sum();

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
        } else { // Fallback for insufficient data
            summary.setTotalUsers(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setNewUsers(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setActiveUsers(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric(0, 0));
            summary.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(0, 0));
        }

        // --- MOCK DATA SECTION ---
        summary.setTotalRevenue(new AnalyticsSummaryDto.Metric(1389456.0, 1267234.0));
        summary.setVipRevenue(new AnalyticsSummaryDto.Metric(834567.0, 756789.0));
        summary.setShopRevenue(new AnalyticsSummaryDto.Metric(554889.0, 510445.0));
        summary.setVipDistribution(Arrays.asList(new AnalyticsSummaryDto.DistributionPoint("Monthly (Mock)", 423)));
        summary.setCategoryRevenue(Arrays.asList(new AnalyticsSummaryDto.DistributionPoint("Electronics (Mock)", 456789)));
        summary.setRevenueGrowthTrend(Arrays.asList(new AnalyticsSummaryDto.RevenueGrowthPoint("Jan", 834567, 554889)));

        return summary;
    }

    @Override
    public Long getRedemptionVolume() {
        return 0L;
    }
}

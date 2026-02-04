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
                // CORRECTED: Replaced yearOfWeekBasedYear() with a simpler, more compatible method.
                int year = weekDate.getYear();
                String period = "Week " + weekNumber + ", " + year;

                List<Ranking> weekRankings = rankingsByPeriod.getOrDefault(period, Collections.emptyList());
                long carbonSaved = weekRankings.stream().mapToLong(Ranking::getCarbonSaved).sum();
                long activeUsers = weekRankings.stream().map(Ranking::getUserId).distinct().count();

                LocalDateTime weekStart = weekDate.with(weekFields.dayOfWeek(), 1).atStartOfDay();
                long newUsers = allUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(weekStart) && u.getCreatedAt().isBefore(weekStart.plusWeeks(1))).count();
                
                userTrend.add(new AnalyticsSummaryDto.UserGrowthPoint("W" + weekNumber, allUsers.size(), newUsers, activeUsers));
                carbonTrend.add(new AnalyticsSummaryDto.CarbonGrowthPoint("W" + weekNumber, carbonSaved, activeUsers > 0 ? (double)carbonSaved/activeUsers : 0));
            }
            Collections.reverse(userTrend);
            Collections.reverse(carbonTrend);

        } else { // Default to monthly
            YearMonth currentMonth = YearMonth.now();
            for (int i = 0; i < 6; i++) { // Last 6 months
                YearMonth month = currentMonth.minusMonths(i);
                final int year = month.getYear();
                final int monthValue = month.getMonthValue();

                long newUsers = allUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().getYear() == year && u.getCreatedAt().getMonthValue() == monthValue).count();
                long activeUsers = allUsers.stream().filter(u -> u.getActivityMetrics() != null && u.getUpdatedAt() != null && u.getUpdatedAt().isAfter(month.atDay(1).atStartOfDay())).count();
                long carbonSaved = rankingsByPeriod.values().stream().flatMap(List::stream)
                    .filter(r -> r.getStartDate() != null && YearMonth.from(r.getStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()).equals(month))
                    .mapToLong(Ranking::getCarbonSaved).sum();

                userTrend.add(new AnalyticsSummaryDto.UserGrowthPoint(month.format(java.time.format.DateTimeFormatter.ofPattern("MMM")), allUsers.size(), newUsers, activeUsers));
                carbonTrend.add(new AnalyticsSummaryDto.CarbonGrowthPoint(month.format(java.time.format.DateTimeFormatter.ofPattern("MMM")), carbonSaved, activeUsers > 0 ? (double)carbonSaved/activeUsers : 0));
            }
            Collections.reverse(userTrend);
            Collections.reverse(carbonTrend);
        }

        summary.setUserGrowthTrend(userTrend);
        summary.setCarbonGrowthTrend(carbonTrend);
        
        AnalyticsSummaryDto.UserGrowthPoint latestUserPoint = userTrend.isEmpty() ? new AnalyticsSummaryDto.UserGrowthPoint("",0,0,0) : userTrend.get(userTrend.size() - 1);
        AnalyticsSummaryDto.CarbonGrowthPoint latestCarbonPoint = carbonTrend.isEmpty() ? new AnalyticsSummaryDto.CarbonGrowthPoint("",0,0) : carbonTrend.get(carbonTrend.size() - 1);
        AnalyticsSummaryDto.UserGrowthPoint previousUserPoint = userTrend.size() > 1 ? userTrend.get(userTrend.size() - 2) : new AnalyticsSummaryDto.UserGrowthPoint("",0,0,0);
        AnalyticsSummaryDto.CarbonGrowthPoint previousCarbonPoint = carbonTrend.size() > 1 ? carbonTrend.get(carbonTrend.size() - 2) : new AnalyticsSummaryDto.CarbonGrowthPoint("",0,0);

        summary.setTotalUsers(new AnalyticsSummaryDto.Metric((double)latestUserPoint.getUsers(), (double)previousUserPoint.getUsers()));
        summary.setNewUsers(new AnalyticsSummaryDto.Metric((double)latestUserPoint.getNewUsers(), (double)previousUserPoint.getNewUsers()));
        summary.setActiveUsers(new AnalyticsSummaryDto.Metric((double)latestUserPoint.getActiveUsers(), (double)previousUserPoint.getActiveUsers()));
        summary.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric((double)latestCarbonPoint.getCarbonSaved(), (double)previousCarbonPoint.getCarbonSaved()));
        summary.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(latestCarbonPoint.getAvgPerUser(), previousCarbonPoint.getAvgPerUser()));

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
        return 0L; // Returning 0 as UserPointsLog might not be fully available
    }
}

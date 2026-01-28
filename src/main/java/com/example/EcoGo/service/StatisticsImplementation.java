package com.example.EcoGo.service;

import com.example.EcoGo.dto.DashboardStatsDTO;
import com.example.EcoGo.dto.HeatmapDataDTO;
import com.example.EcoGo.interfacemethods.StatisticsInterface;
import com.example.EcoGo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsImplementation implements StatisticsInterface {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserPointsLogRepository pointsLogRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 用户统计
        stats.setTotalUsers(0L); // Placeholder
        stats.setActiveUsers(0L); // Placeholder

        // 广告统计
        long totalAds = advertisementRepository.count();
        long activeAds = advertisementRepository.findByStatus("Active").size();
        stats.setTotalAdvertisements(totalAds);
        stats.setActiveAdvertisements(activeAds);

        // 活动统计
        long totalActivities = activityRepository.count();
        long ongoingActivities = activityRepository.findByStatus("ONGOING").size()
                + activityRepository.findByStatus("PUBLISHED").size();
        stats.setTotalActivities(totalActivities);
        stats.setOngoingActivities(ongoingActivities);

        // 碳积分统计
        stats.setTotalCarbonCredits(0L); // Placeholder

        // 碳减排量统计 (Use Points as proxy for now, assumes 1 point = 1g carbon or similar)
        // In reality, we might need a separate CarbonLog if points != carbon
        // For now, assuming "gain" points reflects activity volume
        Long totalReduction = getTotalCarbonReduction();
        stats.setTotalCarbonReduction(totalReduction);

        // 兑换量统计
        Long redemptionVol = getRedemptionVolume();
        stats.setRedemptionVolume(redemptionVol);

        return stats;
    }

    @Override
    public Long getTotalCarbonReduction() {
        // Calculate total points earned as a proxy
        List<com.example.EcoGo.model.UserPointsLog> logs = pointsLogRepository.findAll();
        return logs.stream()
                .filter(log -> "gain".equalsIgnoreCase(log.getChangeType()))
                .mapToLong(com.example.EcoGo.model.UserPointsLog::getPoints)
                .sum();
    }

    @Override
    public Long getActiveUserCount(int days) {
        return 0L;
    }

    @Override
    public Long getRedemptionVolume() {
        List<com.example.EcoGo.model.UserPointsLog> logs = pointsLogRepository.findAll();
        return logs.stream()
                .filter(log -> "redeem".equalsIgnoreCase(log.getChangeType())
                        || "redeem".equalsIgnoreCase(log.getSource()))
                .mapToLong(log -> Math.abs(log.getPoints())) // Points are usually negative or positive depending on
                                                             // implementation
                .sum();
    }

    @Override
    public HeatmapDataDTO.HeatmapSummary getEmissionHeatmap() {
        HeatmapDataDTO.HeatmapSummary summary = new HeatmapDataDTO.HeatmapSummary();
        List<HeatmapDataDTO> dataPoints = generateMockHeatmapData();
        summary.setDataPoints(dataPoints);
        // ... (rest of the method is unchanged)
        return summary;
    }

    // This method is unchanged
    private List<HeatmapDataDTO> generateMockHeatmapData() {
        List<HeatmapDataDTO> dataPoints = new ArrayList<>();
        // ... (rest of the method is unchanged)
        return dataPoints;
    }
}

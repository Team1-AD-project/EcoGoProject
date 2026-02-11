package com.example.EcoGo.controller;

import com.example.EcoGo.dto.AnalyticsSummaryDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.StatisticsInterface;
import com.example.EcoGo.utils.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class StatisticsController {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    private final StatisticsInterface statisticsService;

    public StatisticsController(StatisticsInterface statisticsService) {
        this.statisticsService = statisticsService;
    }

    // === Web Endpoints (Admin) ===

    @GetMapping("/web/statistics/management-analytics")
    public ResponseMessage<AnalyticsSummaryDto> getWebManagementAnalytics(
            @RequestParam(defaultValue = "monthly") String timeRange) {
        logger.info("[WEB] Fetching management analytics for time range: {}", LogSanitizer.sanitize(timeRange));
        AnalyticsSummaryDto summary = statisticsService.getManagementAnalytics(timeRange);
        return ResponseMessage.success(summary);
    }

    @GetMapping("/web/statistics/redemption-volume")
    public ResponseMessage<Long> getWebRedemptionVolume() {
        logger.info("[WEB] Fetching redemption volume");
        Long volume = statisticsService.getRedemptionVolume();
        return ResponseMessage.success(volume);
    }

    // === Mobile Endpoints ===

    @GetMapping("/mobile/statistics/management-analytics")
    public ResponseMessage<AnalyticsSummaryDto> getMobileManagementAnalytics(
            @RequestParam(defaultValue = "monthly") String timeRange) {
        logger.info("[Mobile] Fetching management analytics for time range: {}", LogSanitizer.sanitize(timeRange));
        AnalyticsSummaryDto summary = statisticsService.getManagementAnalytics(timeRange);
        return ResponseMessage.success(summary);
    }

    @GetMapping("/mobile/statistics/redemption-volume")
    public ResponseMessage<Long> getMobileRedemptionVolume() {
        logger.info("[Mobile] Fetching redemption volume");
        Long volume = statisticsService.getRedemptionVolume();
        return ResponseMessage.success(volume);
    }
}

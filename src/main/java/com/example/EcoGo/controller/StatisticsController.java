package com.example.EcoGo.controller;

import com.example.EcoGo.dto.AnalyticsSummaryDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.StatisticsInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @Autowired
    private StatisticsInterface statisticsService;

    @GetMapping("/management-analytics")
    public ResponseMessage<AnalyticsSummaryDto> getManagementAnalytics(
            @RequestParam(defaultValue = "monthly") String timeRange) {
        logger.info("Fetching management analytics for time range: {}", timeRange);
        AnalyticsSummaryDto summary = statisticsService.getManagementAnalytics(timeRange);
        return ResponseMessage.success(summary);
    }

    @GetMapping("/redemption-volume")
    public ResponseMessage<Long> getRedemptionVolume() {
        logger.info("获取奖励兑换量");
        Long volume = statisticsService.getRedemptionVolume();
        return ResponseMessage.success(volume);
    }
}

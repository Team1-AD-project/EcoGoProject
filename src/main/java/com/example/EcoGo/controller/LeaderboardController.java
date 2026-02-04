package com.example.EcoGo.controller;

import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboards")
public class LeaderboardController {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardController.class);

    @Autowired
    private LeaderboardInterface leaderboardService;

    @GetMapping("/periods")
    public ResponseMessage<List<String>> getAvailablePeriods() {
        logger.info("Fetching all available leaderboard periods");
        List<String> periods = leaderboardService.getAvailablePeriods();
        return ResponseMessage.success(periods);
    }

    @GetMapping("/rankings")
    public ResponseMessage<LeaderboardStatsDto> getRankingsByPeriod(
            @RequestParam String period,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching rankings for period: {} with search: '{}'", period, name);
        LeaderboardStatsDto statsDto = leaderboardService.getRankingsAndStatsByPeriod(period, name, page, size);
        return ResponseMessage.success(statsDto);
    }
}

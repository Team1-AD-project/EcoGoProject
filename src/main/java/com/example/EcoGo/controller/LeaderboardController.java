package com.example.EcoGo.controller;

import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class LeaderboardController {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardController.class);

    @Autowired
    private LeaderboardInterface leaderboardService;

    // === Web Endpoints (Admin - can view any date/month) ===

    @GetMapping("/web/leaderboards/rankings")
    public ResponseMessage<LeaderboardStatsDto> getWebRankings(
            @RequestParam String type,
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("[WEB] Fetching {} leaderboard, date: {}, search: '{}'", type, date, name);
        LeaderboardStatsDto statsDto = leaderboardService.getRankings(type, date, name, page, size);
        return ResponseMessage.success(statsDto);
    }

    // === Mobile Endpoints (User - only today/this month, no date param) ===

    @GetMapping("/mobile/leaderboards/rankings")
    public ResponseMessage<LeaderboardStatsDto> getMobileRankings(
            @RequestParam String type,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("[Mobile] Fetching {} leaderboard, search: '{}'", type, name);
        LeaderboardStatsDto statsDto = leaderboardService.getRankings(type, "", name, page, size);
        return ResponseMessage.success(statsDto);
    }
}

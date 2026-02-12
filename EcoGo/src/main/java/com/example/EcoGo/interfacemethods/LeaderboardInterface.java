package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.dto.LeaderboardStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface LeaderboardInterface {

    /**
     * Get real-time leaderboard rankings computed from trips.
     * @param type "DAILY" or "MONTHLY"
     * @param date date string - for DAILY: "2026-02-07", for MONTHLY: "2026-02"
     * @param name nickname search filter (empty for no filter)
     * @param page page number (0-based)
     * @param size page size
     */
    LeaderboardStatsDto getRankings(String type, String date, String name, int page, int size);

    /**
     * Get top N users for a given date range (used by scheduler for rewards).
     */
    List<LeaderboardEntry> getTopUsers(LocalDateTime start, LocalDateTime end, int limit);
}

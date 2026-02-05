package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.model.Ranking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LeaderboardInterface {

    Page<Ranking> getRankingsByPeriod(String period, String name, Pageable pageable);

    LeaderboardStatsDto getRankingsAndStatsByPeriod(String period, String name, int page, int size);

    List<String> getAvailablePeriods();
}

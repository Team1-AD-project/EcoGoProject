package com.example.EcoGo.service;

import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.model.LeaderboardReward;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.LeaderboardRewardRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardImplementationTest {

    @Mock private MongoTemplate mongoTemplate;
    @Mock private UserRepository userRepository;
    @Mock private LeaderboardRewardRepository rewardRepository;

    @InjectMocks private LeaderboardImplementation leaderboardService;

    // ---------- helper ----------
    private static LeaderboardEntry entry(String userId, double carbon) {
        LeaderboardEntry e = new LeaderboardEntry();
        e.setUserId(userId);
        e.setTotalCarbonSaved(carbon);
        return e;
    }

    private static User user(String userid, String nickname, boolean vipActive) {
        User u = new User();
        u.setUserid(userid);
        u.setNickname(nickname);
        if (vipActive) {
            User.Vip vip = new User.Vip();
            vip.setActive(true);
            u.setVip(vip);
        }
        return u;
    }

    @SuppressWarnings("unchecked")
    private void mockAggregation(List<LeaderboardEntry> entries) {
        AggregationResults<LeaderboardEntry> results = mock(AggregationResults.class);
        when(results.getMappedResults()).thenReturn(entries);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("trips"), eq(LeaderboardEntry.class)))
                .thenReturn(results);
    }

    // ---------- getRankings - MONTHLY ----------
    @Test
    void getRankings_monthly_success() {
        LeaderboardEntry e1 = entry("user001", 200.0);
        LeaderboardEntry e2 = entry("user002", 100.0);
        mockAggregation(List.of(e1, e2));

        User u1 = user("user001", "Alice", true);
        User u2 = user("user002", "Bob", false);
        when(userRepository.findByUseridIn(anyList())).thenReturn(List.of(u1, u2));
        when(rewardRepository.findByTypeAndPeriodKey(anyString(), anyString())).thenReturn(List.of());

        LeaderboardStatsDto result = leaderboardService.getRankings("MONTHLY", "2026-02", "", 0, 10);

        assertNotNull(result);
        assertEquals(300L, result.getTotalCarbonSaved());
        assertEquals(1L, result.getTotalVipUsers());
        assertEquals(2, result.getRankingsPage().getTotalElements());
        assertEquals("Alice", result.getRankingsPage().getContent().get(0).getNickname());
    }

    // ---------- getRankings - DAILY ----------
    @Test
    void getRankings_daily_success() {
        LeaderboardEntry e1 = entry("user001", 50.0);
        mockAggregation(List.of(e1));

        User u1 = user("user001", "Alice", false);
        when(userRepository.findByUseridIn(anyList())).thenReturn(List.of(u1));
        when(rewardRepository.findByTypeAndPeriodKey(anyString(), anyString())).thenReturn(List.of());

        LeaderboardStatsDto result = leaderboardService.getRankings("DAILY", "2026-02-07", "", 0, 10);

        assertNotNull(result);
        assertEquals(50L, result.getTotalCarbonSaved());
        assertEquals(0L, result.getTotalVipUsers());
    }

    // ---------- getRankings - empty date ----------
    @Test
    void getRankings_emptyDate_usesCurrentDate() {
        mockAggregation(List.of());
        when(rewardRepository.findByTypeAndPeriodKey(anyString(), anyString())).thenReturn(List.of());

        LeaderboardStatsDto result = leaderboardService.getRankings("MONTHLY", "", "", 0, 10);

        assertNotNull(result);
        assertEquals(0L, result.getTotalCarbonSaved());
        assertEquals(0, result.getRankingsPage().getTotalElements());
    }

    // ---------- getRankings - name filter ----------
    @Test
    void getRankings_withNameFilter() {
        LeaderboardEntry e1 = entry("user001", 200.0);
        LeaderboardEntry e2 = entry("user002", 100.0);
        mockAggregation(List.of(e1, e2));

        User u1 = user("user001", "Alice", false);
        User u2 = user("user002", "Bob", false);
        when(userRepository.findByUseridIn(anyList())).thenReturn(List.of(u1, u2));
        when(rewardRepository.findByTypeAndPeriodKey(anyString(), anyString())).thenReturn(List.of());

        LeaderboardStatsDto result = leaderboardService.getRankings("MONTHLY", "2026-02", "bob", 0, 10);

        assertNotNull(result);
        // Only Bob should be in the page
        assertEquals(1, result.getRankingsPage().getTotalElements());
        assertEquals("Bob", result.getRankingsPage().getContent().get(0).getNickname());
        // But total stats computed from full list
        assertEquals(300L, result.getTotalCarbonSaved());
    }

    // ---------- getRankings - pagination ----------
    @Test
    void getRankings_pagination() {
        LeaderboardEntry e1 = entry("user001", 300.0);
        LeaderboardEntry e2 = entry("user002", 200.0);
        LeaderboardEntry e3 = entry("user003", 100.0);
        mockAggregation(List.of(e1, e2, e3));

        User u1 = user("user001", "Alice", false);
        User u2 = user("user002", "Bob", false);
        User u3 = user("user003", "Charlie", false);
        when(userRepository.findByUseridIn(anyList())).thenReturn(List.of(u1, u2, u3));
        when(rewardRepository.findByTypeAndPeriodKey(anyString(), anyString())).thenReturn(List.of());

        // Request page 1 with size 2 (should get Charlie only)
        LeaderboardStatsDto result = leaderboardService.getRankings("MONTHLY", "2026-02", "", 1, 2);

        assertNotNull(result);
        assertEquals(3, result.getRankingsPage().getTotalElements());
        assertEquals(1, result.getRankingsPage().getContent().size());
        assertEquals("Charlie", result.getRankingsPage().getContent().get(0).getNickname());
    }

    // ---------- getRankings - rewards distributed ----------
    @Test
    void getRankings_rewardsDistributed() {
        mockAggregation(List.of());
        when(rewardRepository.findByTypeAndPeriodKey("MONTHLY", "2026-02"))
                .thenReturn(List.of(new LeaderboardReward(), new LeaderboardReward()));

        LeaderboardStatsDto result = leaderboardService.getRankings("MONTHLY", "2026-02", "", 0, 10);

        assertEquals(2L, result.getTotalRewardsDistributed());
    }

    // ---------- getRankings - user not found in userMap ----------
    @Test
    void getRankings_userNotInUserMap_usesUserIdAsNickname() {
        LeaderboardEntry e1 = entry("unknownUser", 100.0);
        mockAggregation(List.of(e1));

        // User not found in DB
        when(userRepository.findByUseridIn(anyList())).thenReturn(List.of());
        when(rewardRepository.findByTypeAndPeriodKey(anyString(), anyString())).thenReturn(List.of());

        LeaderboardStatsDto result = leaderboardService.getRankings("MONTHLY", "2026-02", "", 0, 10);

        assertEquals("unknownUser", result.getRankingsPage().getContent().get(0).getNickname());
    }

    // ---------- getTopUsers ----------
    @Test
    void getTopUsers_withLimit() {
        LeaderboardEntry e1 = entry("user001", 200.0);
        mockAggregation(List.of(e1));

        List<LeaderboardEntry> result = leaderboardService.getTopUsers(
                java.time.LocalDateTime.of(2026, 2, 1, 0, 0),
                java.time.LocalDateTime.of(2026, 3, 1, 0, 0),
                5
        );

        assertEquals(1, result.size());
        assertEquals("user001", result.get(0).getUserId());
    }

    @Test
    void getTopUsers_noLimit() {
        LeaderboardEntry e1 = entry("user001", 200.0);
        LeaderboardEntry e2 = entry("user002", 100.0);
        mockAggregation(List.of(e1, e2));

        List<LeaderboardEntry> result = leaderboardService.getTopUsers(
                java.time.LocalDateTime.of(2026, 2, 1, 0, 0),
                java.time.LocalDateTime.of(2026, 3, 1, 0, 0),
                0
        );

        assertEquals(2, result.size());
    }
}

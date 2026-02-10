package com.example.EcoGo.service;

import com.example.EcoGo.dto.AnalyticsSummaryDto;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsImplementationTest {

    @Mock private UserRepository userRepository;
    @Mock private TripRepository tripRepository;

    @InjectMocks private StatisticsImplementation statisticsService;

    // ---------- helper ----------
    private static User buildUser(String userid, String nickname, boolean isAdmin,
                                  LocalDateTime createdAt, LocalDateTime updatedAt,
                                  boolean vipActive, int activeDays30d) {
        User u = new User();
        u.setUserid(userid);
        u.setNickname(nickname);
        u.setAdmin(isAdmin);
        u.setCreatedAt(createdAt);
        u.setUpdatedAt(updatedAt);

        if (vipActive) {
            User.Vip vip = new User.Vip();
            vip.setActive(true);
            u.setVip(vip);
        }

        User.ActivityMetrics metrics = new User.ActivityMetrics();
        metrics.setActiveDays30d(activeDays30d);
        metrics.setActiveDays7d(activeDays30d > 0 ? 3 : 0);
        u.setActivityMetrics(metrics);

        return u;
    }

    private static Trip buildTrip(double carbonSaved) {
        Trip t = new Trip();
        t.setCarbonSaved(carbonSaved);
        return t;
    }

    // ---------- getManagementAnalytics - monthly ----------
    @Test
    void getManagementAnalytics_monthly_success() {
        LocalDateTime now = LocalDateTime.now();
        User u1 = buildUser("user001", "Alice", false, now.minusMonths(2), now, true, 5);
        User u2 = buildUser("user002", "Bob", false, now.minusDays(10), now, false, 3);
        User admin = buildUser("admin001", "Admin", true, now.minusMonths(6), now, false, 0);

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, admin));
        // Return trips for any time range
        when(tripRepository.findByStartTimeBetweenAndCarbonStatus(any(), any(), eq("completed")))
                .thenReturn(List.of(buildTrip(50.0), buildTrip(30.0)));

        AnalyticsSummaryDto result = statisticsService.getManagementAnalytics("monthly");

        assertNotNull(result);
        assertNotNull(result.getTotalUsers());
        assertNotNull(result.getNewUsers());
        assertNotNull(result.getActiveUsers());
        assertNotNull(result.getTotalCarbonSaved());
        assertNotNull(result.getUserGrowthTrend());
        assertNotNull(result.getCarbonGrowthTrend());
        // Should have 6 data points for monthly
        assertEquals(6, result.getUserGrowthTrend().size());
        assertEquals(6, result.getCarbonGrowthTrend().size());
    }

    // ---------- getManagementAnalytics - weekly ----------
    @Test
    void getManagementAnalytics_weekly_success() {
        LocalDateTime now = LocalDateTime.now();
        User u1 = buildUser("user001", "Alice", false, now.minusWeeks(2), now, false, 0);

        when(userRepository.findAll()).thenReturn(List.of(u1));
        when(tripRepository.findByStartTimeBetweenAndCarbonStatus(any(), any(), eq("completed")))
                .thenReturn(List.of(buildTrip(20.0)));

        AnalyticsSummaryDto result = statisticsService.getManagementAnalytics("weekly");

        assertNotNull(result);
        // Should have 5 data points for weekly
        assertEquals(5, result.getUserGrowthTrend().size());
        assertEquals(5, result.getCarbonGrowthTrend().size());
    }

    // ---------- getManagementAnalytics - admin users excluded ----------
    @Test
    void getManagementAnalytics_excludesAdminUsers() {
        LocalDateTime now = LocalDateTime.now();
        User admin = buildUser("admin001", "Admin", true, now.minusDays(1), now, false, 0);

        when(userRepository.findAll()).thenReturn(List.of(admin));
        when(tripRepository.findByStartTimeBetweenAndCarbonStatus(any(), any(), eq("completed")))
                .thenReturn(List.of());

        AnalyticsSummaryDto result = statisticsService.getManagementAnalytics("monthly");

        assertNotNull(result);
        // Admin excluded, so no users should be counted
        // The latest trend point should show 0 users
        if (!result.getUserGrowthTrend().isEmpty()) {
            AnalyticsSummaryDto.UserGrowthPoint latest =
                    result.getUserGrowthTrend().get(result.getUserGrowthTrend().size() - 1);
            assertEquals(0, latest.getUsers());
        }
    }

    // ---------- getManagementAnalytics - VIP distribution ----------
    @Test
    void getManagementAnalytics_vipDistribution() {
        LocalDateTime now = LocalDateTime.now();
        User vipUser = buildUser("user001", "Alice", false, now.minusMonths(1), now, true, 5);
        User nonVip = buildUser("user002", "Bob", false, now.minusMonths(1), now, false, 3);

        when(userRepository.findAll()).thenReturn(List.of(vipUser, nonVip));
        when(tripRepository.findByStartTimeBetweenAndCarbonStatus(any(), any(), eq("completed")))
                .thenReturn(List.of());

        AnalyticsSummaryDto result = statisticsService.getManagementAnalytics("monthly");

        assertNotNull(result.getVipDistribution());
        assertEquals(2, result.getVipDistribution().size());
        // First should be VIP Active
        assertEquals("VIP Active", result.getVipDistribution().get(0).getName());
        assertEquals(1, result.getVipDistribution().get(0).getValue());
        // Second should be Non-VIP
        assertEquals("Non-VIP", result.getVipDistribution().get(1).getName());
        assertEquals(1, result.getVipDistribution().get(1).getValue());
    }

    // ---------- getManagementAnalytics - empty users ----------
    @Test
    void getManagementAnalytics_noUsers() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(tripRepository.findByStartTimeBetweenAndCarbonStatus(any(), any(), eq("completed")))
                .thenReturn(List.of());

        AnalyticsSummaryDto result = statisticsService.getManagementAnalytics("monthly");

        assertNotNull(result);
        assertNotNull(result.getVipDistribution());
        assertEquals(0, result.getVipDistribution().get(0).getValue()); // VIP Active = 0
        assertEquals(0, result.getVipDistribution().get(1).getValue()); // Non-VIP = 0
    }

    // ---------- getManagementAnalytics - userRepository throws exception ----------
    @Test
    void getManagementAnalytics_dbError_returnsEmptyDto() {
        when(userRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        AnalyticsSummaryDto result = statisticsService.getManagementAnalytics("monthly");

        assertNotNull(result);
        assertEquals(0, result.getTotalUsers().getCurrentValue());
        assertEquals(0, result.getNewUsers().getCurrentValue());
        assertTrue(result.getUserGrowthTrend().isEmpty());
    }

    // ---------- getManagementAnalytics - revenue placeholders ----------
    @Test
    void getManagementAnalytics_revenuePlaceholders() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(tripRepository.findByStartTimeBetweenAndCarbonStatus(any(), any(), eq("completed")))
                .thenReturn(List.of());

        AnalyticsSummaryDto result = statisticsService.getManagementAnalytics("monthly");

        assertEquals(0, result.getTotalRevenue().getCurrentValue());
        assertEquals(0, result.getVipRevenue().getCurrentValue());
        assertEquals(0, result.getShopRevenue().getCurrentValue());
        assertTrue(result.getCategoryRevenue().isEmpty());
        assertTrue(result.getRevenueGrowthTrend().isEmpty());
    }

    // ---------- getRedemptionVolume ----------
    @Test
    void getRedemptionVolume_returnsZero() {
        Long result = statisticsService.getRedemptionVolume();
        assertEquals(0L, result);
    }
}

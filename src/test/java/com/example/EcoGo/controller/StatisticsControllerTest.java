package com.example.EcoGo.controller;

import com.example.EcoGo.dto.AnalyticsSummaryDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.StatisticsInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsControllerTest {

    private StatisticsInterface statisticsService;
    private StatisticsController controller;

    @BeforeEach
    void setUp() {
        statisticsService = mock(StatisticsInterface.class);
        controller = new StatisticsController(statisticsService);
    }

    // ---------- helper ----------
    private static AnalyticsSummaryDto buildSummary() {
        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.setTotalUsers(new AnalyticsSummaryDto.Metric(100, 80));
        dto.setNewUsers(new AnalyticsSummaryDto.Metric(20, 15));
        dto.setActiveUsers(new AnalyticsSummaryDto.Metric(60, 50));
        dto.setTotalCarbonSaved(new AnalyticsSummaryDto.Metric(5000, 3000));
        dto.setAverageCarbonPerUser(new AnalyticsSummaryDto.Metric(83.3, 60.0));
        dto.setTotalRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        dto.setVipRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        dto.setShopRevenue(new AnalyticsSummaryDto.Metric(0, 0));
        dto.setUserGrowthTrend(new ArrayList<>());
        dto.setCarbonGrowthTrend(new ArrayList<>());
        dto.setRevenueGrowthTrend(new ArrayList<>());
        dto.setVipDistribution(Arrays.asList(
                new AnalyticsSummaryDto.DistributionPoint("VIP Active", 30),
                new AnalyticsSummaryDto.DistributionPoint("Non-VIP", 70)
        ));
        dto.setCategoryRevenue(new ArrayList<>());
        return dto;
    }

    // ---------- getWebManagementAnalytics ----------
    @Test
    void getWebManagementAnalytics_monthly() {
        AnalyticsSummaryDto summary = buildSummary();
        when(statisticsService.getManagementAnalytics("monthly")).thenReturn(summary);

        ResponseMessage<AnalyticsSummaryDto> resp = controller.getWebManagementAnalytics("monthly");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(100, resp.getData().getTotalUsers().getCurrentValue());
        assertEquals(20, resp.getData().getNewUsers().getCurrentValue());
        verify(statisticsService).getManagementAnalytics("monthly");
    }

    @Test
    void getWebManagementAnalytics_weekly() {
        AnalyticsSummaryDto summary = buildSummary();
        when(statisticsService.getManagementAnalytics("weekly")).thenReturn(summary);

        ResponseMessage<AnalyticsSummaryDto> resp = controller.getWebManagementAnalytics("weekly");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(statisticsService).getManagementAnalytics("weekly");
    }

    @Test
    void getWebManagementAnalytics_vipDistribution() {
        AnalyticsSummaryDto summary = buildSummary();
        when(statisticsService.getManagementAnalytics("monthly")).thenReturn(summary);

        ResponseMessage<AnalyticsSummaryDto> resp = controller.getWebManagementAnalytics("monthly");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(2, resp.getData().getVipDistribution().size());
        assertEquals("VIP Active", resp.getData().getVipDistribution().get(0).getName());
        assertEquals(30, resp.getData().getVipDistribution().get(0).getValue());
    }

    // ---------- getWebRedemptionVolume ----------
    @Test
    void getWebRedemptionVolume_success() {
        when(statisticsService.getRedemptionVolume()).thenReturn(42L);

        ResponseMessage<Long> resp = controller.getWebRedemptionVolume();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(42L, resp.getData());
        verify(statisticsService).getRedemptionVolume();
    }

    @Test
    void getWebRedemptionVolume_zero() {
        when(statisticsService.getRedemptionVolume()).thenReturn(0L);

        ResponseMessage<Long> resp = controller.getWebRedemptionVolume();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(0L, resp.getData());
    }

    // ---------- getMobileManagementAnalytics ----------
    @Test
    void getMobileManagementAnalytics_success() {
        AnalyticsSummaryDto summary = buildSummary();
        when(statisticsService.getManagementAnalytics("monthly")).thenReturn(summary);

        ResponseMessage<AnalyticsSummaryDto> resp = controller.getMobileManagementAnalytics("monthly");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(5000, resp.getData().getTotalCarbonSaved().getCurrentValue());
        verify(statisticsService).getManagementAnalytics("monthly");
    }

    // ---------- getMobileRedemptionVolume ----------
    @Test
    void getMobileRedemptionVolume_success() {
        when(statisticsService.getRedemptionVolume()).thenReturn(100L);

        ResponseMessage<Long> resp = controller.getMobileRedemptionVolume();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(100L, resp.getData());
        verify(statisticsService).getRedemptionVolume();
    }
}

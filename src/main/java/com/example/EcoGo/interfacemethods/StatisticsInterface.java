package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.AnalyticsSummaryDto;

public interface StatisticsInterface {

    /**
     * Retrieves all aggregated data needed for the management analytics page.
     * @param timeRange The time range identifier (e.g., "daily", "weekly", "monthly").
     * @return A DTO containing all the calculated metrics and trends.
     */
    AnalyticsSummaryDto getManagementAnalytics(String timeRange);

    /**
     * Gets the total volume of points redeemed.
     */
    Long getRedemptionVolume();
}

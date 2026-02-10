package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.AnalyticsSummaryDto;

public interface StatisticsInterface {

    AnalyticsSummaryDto getManagementAnalytics(String timeRange);

    Long getRedemptionVolume();
}

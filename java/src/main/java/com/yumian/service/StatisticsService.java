package com.yumian.service;

import java.util.Map;

public interface StatisticsService {
    Map<String, Object> getOverview(Long userId);
    Map<String, Object> getTrend(Long userId, String startDate, String endDate);
    Map<String, Object> getOverview(Long userId, String startDate, String endDate);
}

package com.yumian.controller;

import com.yumian.common.Result;
import com.yumian.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(@RequestAttribute("userId") Long userId) {
        return Result.success(statisticsService.getOverview(userId));
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestAttribute("userId") Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.success(statisticsService.getTrend(userId, startDate, endDate));
    }

    @GetMapping("/overview/range")
    public Result<Map<String, Object>> getOverviewRange(
            @RequestAttribute("userId") Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.success(statisticsService.getOverview(userId, startDate, endDate));
    }
}

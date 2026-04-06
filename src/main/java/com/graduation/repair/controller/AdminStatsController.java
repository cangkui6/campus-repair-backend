package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.vo.CategoryStatItemVO;
import com.graduation.repair.domain.vo.EfficiencyStatsVO;
import com.graduation.repair.domain.vo.OverviewStatsVO;
import com.graduation.repair.domain.vo.WorkerLoadStatItemVO;
import com.graduation.repair.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewStatsVO> overview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        return ApiResponse.success(statsService.overview(startDate, endDate));
    }

    @GetMapping("/category-distribution")
    public ApiResponse<List<CategoryStatItemVO>> categoryDistribution(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        return ApiResponse.success(statsService.categoryDistribution(startDate, endDate));
    }

    @GetMapping("/efficiency")
    public ApiResponse<EfficiencyStatsVO> efficiency(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        return ApiResponse.success(statsService.efficiency(startDate, endDate));
    }

    @GetMapping("/worker-load")
    public ApiResponse<List<WorkerLoadStatItemVO>> workerLoad(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        return ApiResponse.success(statsService.workerLoad(startDate, endDate));
    }
}

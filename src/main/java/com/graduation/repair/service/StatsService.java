package com.graduation.repair.service;

import com.graduation.repair.domain.vo.CategoryStatItemVO;
import com.graduation.repair.domain.vo.EfficiencyStatsVO;
import com.graduation.repair.domain.vo.OverviewStatsVO;
import com.graduation.repair.domain.vo.WorkerLoadStatItemVO;

import java.time.LocalDate;
import java.util.List;

public interface StatsService {

    OverviewStatsVO overview(LocalDate startDate, LocalDate endDate);

    List<CategoryStatItemVO> categoryDistribution(LocalDate startDate, LocalDate endDate);

    EfficiencyStatsVO efficiency(LocalDate startDate, LocalDate endDate);

    List<WorkerLoadStatItemVO> workerLoad(LocalDate startDate, LocalDate endDate);
}

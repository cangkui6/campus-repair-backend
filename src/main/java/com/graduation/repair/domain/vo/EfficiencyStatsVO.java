package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class EfficiencyStatsVO {

    private Double avgResponseMinutes;
    private Double avgCompleteHours;
}

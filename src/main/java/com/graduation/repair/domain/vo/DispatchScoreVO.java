package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DispatchScoreVO {

    private Long workerId;
    private Double scoreSkill;
    private Double scoreArea;
    private Double scoreLoad;
    private Double scorePerf;
    private Double scoreUrgency;
    private Double totalScore;
    private Integer scoreVersion;
}

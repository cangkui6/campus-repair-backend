package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DispatchWeightConfigVO {

    private Integer versionNo;
    private Double weightSkill;
    private Double weightArea;
    private Double weightLoad;
    private Double weightPerf;
    private Double weightUrgency;
    private String triggerSource;
    private Boolean active;
}

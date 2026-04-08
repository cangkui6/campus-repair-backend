package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class DispatchWeightUpdateRequest {

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double weightSkill;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double weightArea;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double weightLoad;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double weightPerf;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double weightUrgency;
}

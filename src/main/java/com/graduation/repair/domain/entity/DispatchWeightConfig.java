package com.graduation.repair.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispatch_weight_config")
public class DispatchWeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "weight_skill", nullable = false, precision = 6, scale = 4)
    private BigDecimal weightSkill;

    @Column(name = "weight_area", nullable = false, precision = 6, scale = 4)
    private BigDecimal weightArea;

    @Column(name = "weight_load", nullable = false, precision = 6, scale = 4)
    private BigDecimal weightLoad;

    @Column(name = "weight_perf", nullable = false, precision = 6, scale = 4)
    private BigDecimal weightPerf;

    @Column(name = "weight_urgency", nullable = false, precision = 6, scale = 4)
    private BigDecimal weightUrgency;

    @Column(name = "trigger_source", nullable = false, length = 30)
    private String triggerSource;

    @Column(name = "is_active", nullable = false)
    private Integer isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

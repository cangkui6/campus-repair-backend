package com.graduation.repair.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispatch_feedback_snapshot")
public class DispatchFeedbackSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "dispatch_count", nullable = false)
    private Integer dispatchCount;

    @Column(name = "reassign_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal reassignRate;

    @Column(name = "reject_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal rejectRate;

    @Column(name = "timeout_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal timeoutRate;

    @Column(name = "avg_complete_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal avgCompleteHours;

    @Column(name = "applied_version", nullable = false)
    private Integer appliedVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

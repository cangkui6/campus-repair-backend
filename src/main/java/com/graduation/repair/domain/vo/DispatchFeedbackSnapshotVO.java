package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class DispatchFeedbackSnapshotVO {

    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer dispatchCount;
    private Double reassignRate;
    private Double rejectRate;
    private Double timeoutRate;
    private Double avgCompleteHours;
    private Integer appliedVersion;
    private LocalDateTime createdAt;
}

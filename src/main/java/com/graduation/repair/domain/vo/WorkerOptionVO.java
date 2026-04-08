package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WorkerOptionVO {

    private Long workerId;
    private Long userId;
    private String workerName;
    private String skillTags;
    private String serviceArea;
    private Integer currentLoad;
    private Integer isAvailable;
    private java.math.BigDecimal avgCompleteHours;
    private java.math.BigDecimal acceptRate;
    private Integer completedTicketCount;
    private Integer reassignCount;
    private java.time.LocalDateTime lastActiveAt;
}

package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class WorkerProfileVO {

    private Long workerId;
    private String workerName;
    private String skillTags;
    private String serviceArea;
    private Integer currentLoad;
    private BigDecimal avgCompleteHours;
    private BigDecimal acceptRate;
    private Integer completedTicketCount;
    private Integer reassignCount;
    private LocalDateTime lastActiveAt;
    private Long processingCount;
    private Long historyCount;
}

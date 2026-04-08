package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class WorkerHistoryItemVO {

    private Long ticketId;
    private String ticketNo;
    private String status;
    private String locationText;
    private String faultDesc;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
}

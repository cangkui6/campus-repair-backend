package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class WorkerTaskListItemVO {

    private Long ticketId;
    private String ticketNo;
    private String status;
    private String locationText;
    private String faultDesc;
    private String urgencyLevel;
    private LocalDateTime submittedAt;
}

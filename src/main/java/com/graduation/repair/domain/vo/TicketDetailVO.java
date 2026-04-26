package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class TicketDetailVO {

    private Long ticketId;
    private String ticketNo;
    private String rawText;
    private String locationText;
    private String faultDesc;
    private String urgencyLevel;
    private Long categoryId;
    private String status;
    private String contactMasked;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private String repairResult;
    private Integer evaluationScore;
    private String evaluationComment;
    private LocalDateTime evaluatedAt;
    private LlmTraceVO llmTrace;
}

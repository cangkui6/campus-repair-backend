package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ReporterEvaluationItemVO {

    private Long ticketId;
    private String ticketNo;
    private Integer score;
    private String comment;
    private String ticketStatus;
    private LocalDateTime evaluatedAt;
}

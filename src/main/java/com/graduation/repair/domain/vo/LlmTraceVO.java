package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class LlmTraceVO {

    private Long auditLogId;
    private String modelName;
    private String promptVersion;
    private Boolean ragEnabled;
    private Integer ragHitCount;
    private Double confidence;
    private String parseStatus;
    private String failureReason;
    private String rawResponse;
    private String normalizedResult;
    private LocalDateTime createdAt;
}

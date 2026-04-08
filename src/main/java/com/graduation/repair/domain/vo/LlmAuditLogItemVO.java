package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class LlmAuditLogItemVO {

    private Long id;
    private Long ticketId;
    private String ticketNo;
    private String promptVersion;
    private String providerName;
    private String modelName;
    private Long latencyMs;
    private String parseStatus;
    private String rawResponse;
    private String normalizedResult;
    private LocalDateTime createdAt;
}

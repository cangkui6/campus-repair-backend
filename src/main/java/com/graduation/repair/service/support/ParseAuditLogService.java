package com.graduation.repair.service.support;

import com.graduation.repair.domain.entity.LlmParseAuditLog;
import com.graduation.repair.repository.LlmParseAuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ParseAuditLogService {

    private final LlmParseAuditLogRepository auditLogRepository;

    public ParseAuditLogService(LlmParseAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void save(Long ticketId,
                     Long operatorId,
                     String promptVersion,
                     String modelName,
                     Long latencyMs,
                     String rawResponse,
                     String parseStatus,
                     String normalizedResult) {
        LlmParseAuditLog log = new LlmParseAuditLog();
        log.setTicketId(ticketId);
        log.setOperatorId(operatorId);
        log.setPromptVersion(promptVersion);
        log.setModelName(modelName);
        log.setLatencyMs(latencyMs == null ? 0L : latencyMs);
        log.setRawResponse(rawResponse);
        log.setParseStatus(parseStatus);
        log.setNormalizedResult(normalizedResult);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}

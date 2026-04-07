package com.graduation.repair.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "llm_parse_audit_log")
public class LlmParseAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Lob
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "parse_status", nullable = false, length = 30)
    private String parseStatus;

    @Lob
    @Column(name = "normalized_result", columnDefinition = "TEXT")
    private String normalizedResult;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

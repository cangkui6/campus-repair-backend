package com.graduation.repair.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "llm_parse_review_queue")
public class LlmParseReviewQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "queue_status", nullable = false, length = 30)
    private String queueStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

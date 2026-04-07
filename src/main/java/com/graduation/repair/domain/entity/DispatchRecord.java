package com.graduation.repair.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispatch_record")
public class DispatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "worker_id", nullable = false)
    private Long workerId;

    @Column(name = "score_skill", precision = 5, scale = 2)
    private BigDecimal scoreSkill;

    @Column(name = "score_area", precision = 5, scale = 2)
    private BigDecimal scoreArea;

    @Column(name = "score_load", precision = 5, scale = 2)
    private BigDecimal scoreLoad;

    @Column(name = "score_perf", precision = 5, scale = 2)
    private BigDecimal scorePerf;

    @Column(name = "score_urgency", precision = 5, scale = 2)
    private BigDecimal scoreUrgency;

    @Column(name = "total_score", precision = 6, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "score_version")
    private Integer scoreVersion;

    @Column(name = "dispatch_type", nullable = false, length = 20)
    private String dispatchType;

    @Column(name = "dispatch_status", nullable = false, length = 20)
    private String dispatchStatus;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

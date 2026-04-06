package com.graduation.repair.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "repair_ticket")
public class RepairTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_no", nullable = false, unique = true, length = 32)
    private String ticketNo;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Lob
    @Column(name = "raw_text", nullable = false)
    private String rawText;

    @Column(name = "location_text", length = 255)
    private String locationText;

    @Column(name = "device_type", length = 100)
    private String deviceType;

    @Column(name = "fault_desc", length = 500)
    private String faultDesc;

    @Column(name = "urgency_level", length = 20)
    private String urgencyLevel;

    @Column(name = "contact_masked", length = 32)
    private String contactMasked;

    @Column(name = "time_preference", length = 100)
    private String timePreference;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "current_worker_id")
    private Long currentWorkerId;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

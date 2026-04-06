package com.graduation.repair.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "maintenance_worker")
public class MaintenanceWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "skill_tags", nullable = false, length = 255)
    private String skillTags;

    @Column(name = "service_area", nullable = false, length = 255)
    private String serviceArea;

    @Column(name = "current_load", nullable = false)
    private Integer currentLoad;

    @Column(name = "is_available", nullable = false)
    private Integer isAvailable;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

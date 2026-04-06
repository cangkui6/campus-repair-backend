package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.RepairTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RepairTicketRepository extends JpaRepository<RepairTicket, Long> {

    Page<RepairTicket> findByReporterIdOrderBySubmittedAtDesc(Long reporterId, Pageable pageable);

    Page<RepairTicket> findByReporterIdAndStatusOrderBySubmittedAtDesc(Long reporterId, String status, Pageable pageable);

    Page<RepairTicket> findByCurrentWorkerIdOrderBySubmittedAtDesc(Long currentWorkerId, Pageable pageable);

    Page<RepairTicket> findByCurrentWorkerIdAndStatusOrderBySubmittedAtDesc(Long currentWorkerId, String status, Pageable pageable);

    Page<RepairTicket> findByStatusOrderBySubmittedAtDesc(String status, Pageable pageable);

    Page<RepairTicket> findByCategoryIdOrderBySubmittedAtDesc(Long categoryId, Pageable pageable);

    Page<RepairTicket> findByStatusAndCategoryIdOrderBySubmittedAtDesc(String status, Long categoryId, Pageable pageable);

    Page<RepairTicket> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    List<RepairTicket> findBySubmittedAtBetween(LocalDateTime start, LocalDateTime end);
}

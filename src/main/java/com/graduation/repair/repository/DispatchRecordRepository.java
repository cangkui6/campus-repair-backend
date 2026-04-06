package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.DispatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DispatchRecordRepository extends JpaRepository<DispatchRecord, Long> {

    List<DispatchRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    void deleteByTicketId(Long ticketId);
}

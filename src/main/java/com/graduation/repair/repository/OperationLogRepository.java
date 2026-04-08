package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    java.util.List<OperationLog> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    void deleteByTicketId(Long ticketId);
}

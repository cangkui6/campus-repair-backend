package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.LlmParseAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmParseAuditLogRepository extends JpaRepository<LlmParseAuditLog, Long> {

    Page<LlmParseAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<LlmParseAuditLog> findByParseStatusOrderByCreatedAtDesc(String parseStatus, Pageable pageable);

    Page<LlmParseAuditLog> findByModelNameContainingIgnoreCaseOrderByCreatedAtDesc(String modelName, Pageable pageable);

    Page<LlmParseAuditLog> findByPromptVersionOrderByCreatedAtDesc(String promptVersion, Pageable pageable);

    java.util.Optional<LlmParseAuditLog> findTopByTicketIdOrderByCreatedAtDesc(Long ticketId);
}

package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.NotificationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {

    Page<NotificationMessage> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsRead(Long receiverId, Integer isRead);

    void deleteByTicketId(Long ticketId);
}

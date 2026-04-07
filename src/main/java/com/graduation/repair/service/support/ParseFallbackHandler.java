package com.graduation.repair.service.support;

import com.graduation.repair.domain.entity.LlmParseReviewQueue;
import com.graduation.repair.repository.LlmParseReviewQueueRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ParseFallbackHandler {

    private final LlmParseReviewQueueRepository reviewQueueRepository;

    public ParseFallbackHandler(LlmParseReviewQueueRepository reviewQueueRepository) {
        this.reviewQueueRepository = reviewQueueRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(Long ticketId, Long operatorId, String rawText, ParseFailureReason reason) {
        LlmParseReviewQueue item = new LlmParseReviewQueue();
        item.setTicketId(ticketId);
        item.setOperatorId(operatorId);
        item.setRawText(rawText == null ? "" : rawText);
        item.setReasonCode(reason.code());
        item.setReason(reason.message());
        item.setQueueStatus("PENDING_MANUAL_REVIEW");
        item.setCreatedAt(LocalDateTime.now());
        reviewQueueRepository.save(item);
    }
}

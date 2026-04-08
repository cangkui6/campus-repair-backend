package com.graduation.repair.service;

import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.ManualParseConfirmRequest;
import com.graduation.repair.domain.vo.LlmAuditLogItemVO;
import com.graduation.repair.domain.vo.ReviewQueueItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;

import java.time.LocalDateTime;

public interface LlmAdminService {

    PageResult<ReviewQueueItemVO> reviewQueue(Integer page, Integer size, String queueStatus);

    TicketStatusChangeResponse confirmReview(Long operatorId, Long queueId, ManualParseConfirmRequest request);

    PageResult<LlmAuditLogItemVO> auditLogs(Integer page, Integer size, String ticketNo, String parseStatus, String modelName, String promptVersion,
                                            LocalDateTime startTime, LocalDateTime endTime);
}

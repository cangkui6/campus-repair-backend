package com.graduation.repair.service.impl;

import com.graduation.repair.common.enums.TicketStatus;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.ManualParseConfirmRequest;
import com.graduation.repair.domain.entity.LlmParseAuditLog;
import com.graduation.repair.domain.entity.LlmParseReviewQueue;
import com.graduation.repair.domain.entity.OperationLog;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.LlmAuditLogItemVO;
import com.graduation.repair.domain.vo.ReviewQueueItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;
import com.graduation.repair.repository.LlmParseAuditLogRepository;
import com.graduation.repair.repository.LlmParseReviewQueueRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.LlmAdminService;
import com.graduation.repair.service.support.TicketStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LlmAdminServiceImpl implements LlmAdminService {

    private final LlmParseReviewQueueRepository reviewQueueRepository;
    private final LlmParseAuditLogRepository auditLogRepository;
    private final RepairTicketRepository repairTicketRepository;
    private final OperationLogRepository operationLogRepository;
    private final TicketStateMachine ticketStateMachine;

    public LlmAdminServiceImpl(LlmParseReviewQueueRepository reviewQueueRepository,
                               LlmParseAuditLogRepository auditLogRepository,
                               RepairTicketRepository repairTicketRepository,
                               OperationLogRepository operationLogRepository,
                               TicketStateMachine ticketStateMachine) {
        this.reviewQueueRepository = reviewQueueRepository;
        this.auditLogRepository = auditLogRepository;
        this.repairTicketRepository = repairTicketRepository;
        this.operationLogRepository = operationLogRepository;
        this.ticketStateMachine = ticketStateMachine;
    }

    @Override
    public PageResult<ReviewQueueItemVO> reviewQueue(Integer page, Integer size, String queueStatus) {
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);
        List<LlmParseReviewQueue> all = reviewQueueRepository.findAll().stream()
                .filter(item -> queueStatus == null || queueStatus.isBlank() || queueStatus.trim().equals(item.getQueueStatus()))
                .sorted(Comparator.comparing(LlmParseReviewQueue::getCreatedAt).reversed())
                .toList();
        Map<Long, RepairTicket> ticketMap = repairTicketRepository.findAllById(all.stream().map(LlmParseReviewQueue::getTicketId).toList())
                .stream().collect(Collectors.toMap(RepairTicket::getId, Function.identity()));
        List<ReviewQueueItemVO> pageRecords = paginate(all, pageNo, pageSize).stream().map(item -> ReviewQueueItemVO.builder()
                .id(item.getId())
                .ticketId(item.getTicketId())
                .ticketNo(ticketMap.containsKey(item.getTicketId()) ? ticketMap.get(item.getTicketId()).getTicketNo() : "-")
                .rawText(item.getRawText())
                .reasonCode(item.getReasonCode())
                .reason(item.getReason())
                .queueStatus(item.getQueueStatus())
                .createdAt(item.getCreatedAt())
                .build()).toList();
        return new PageResult<>(all.size(), pageNo, pageSize, pageRecords);
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse confirmReview(Long operatorId, Long queueId, ManualParseConfirmRequest request) {
        LlmParseReviewQueue queue = reviewQueueRepository.findById(queueId)
                .orElseThrow(() -> new BizException(4045, "人工确认记录不存在"));
        RepairTicket ticket = repairTicketRepository.findById(queue.getTicketId())
                .orElseThrow(() -> new BizException(4041, "工单不存在"));
        String fromStatus = ticket.getStatus();

        if (!"PENDING_MANUAL_REVIEW".equals(queue.getQueueStatus())) {
            throw new BizException(4001, "该人工确认记录已处理");
        }
        if (!TicketStatus.MANUAL_REVIEW.getValue().equals(fromStatus)) {
            throw new BizException(4001, "仅待人工确认工单允许人工确认后进入待分配");
        }
        if (!ticketStateMachine.canTransit(fromStatus, TicketStatus.PENDING_DISPATCH.getValue())) {
            throw new BizException(4001, "当前状态不允许人工确认后进入待分配");
        }

        ticket.setLocationText(request.getLocationText());
        ticket.setFaultDesc(request.getFaultDesc());
        ticket.setUrgencyLevel(request.getUrgencyLevel());
        ticket.setCategoryId(request.getCategoryId());
        ticket.setStatus(TicketStatus.PENDING_DISPATCH.getValue());
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);

        queue.setQueueStatus("MANUAL_CONFIRMED");
        reviewQueueRepository.save(queue);

        OperationLog log = new OperationLog();
        log.setTicketId(ticket.getId());
        log.setOperatorId(operatorId);
        log.setAction("MANUAL_PARSE_CONFIRM");
        log.setFromStatus(fromStatus);
        log.setToStatus(TicketStatus.PENDING_DISPATCH.getValue());
        log.setDetail("管理员人工补录解析结果，工单进入待分配池");
        log.setCreatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
        return new TicketStatusChangeResponse(ticket.getId(), fromStatus, TicketStatus.PENDING_DISPATCH.getValue());
    }

    @Override
    public PageResult<LlmAuditLogItemVO> auditLogs(Integer page, Integer size, String ticketNo, String parseStatus, String modelName,
                                                   String promptVersion, LocalDateTime startTime, LocalDateTime endTime) {
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);
        List<LlmParseAuditLog> all = auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(LlmParseAuditLog::getCreatedAt).reversed())
                .toList();
        Map<Long, RepairTicket> ticketMap = repairTicketRepository.findAllById(all.stream().map(LlmParseAuditLog::getTicketId).toList())
                .stream().collect(Collectors.toMap(RepairTicket::getId, Function.identity()));
        List<LlmAuditLogItemVO> filtered = all.stream().map(item -> {
            RepairTicket ticket = ticketMap.get(item.getTicketId());
            return LlmAuditLogItemVO.builder()
                    .id(item.getId())
                    .ticketId(item.getTicketId())
                    .ticketNo(ticket == null ? "-" : ticket.getTicketNo())
                    .promptVersion(item.getPromptVersion())
                    .providerName(item.getProviderName())
                    .modelName(item.getModelName())
                    .latencyMs(item.getLatencyMs())
                    .parseStatus(item.getParseStatus())
                    .rawResponse(item.getRawResponse())
                    .normalizedResult(item.getNormalizedResult())
                    .createdAt(item.getCreatedAt())
                    .build();
        }).filter(item -> ticketNo == null || ticketNo.isBlank() || item.getTicketNo().contains(ticketNo.trim()))
                .filter(item -> parseStatus == null || parseStatus.isBlank() || parseStatus.trim().equals(item.getParseStatus()))
                .filter(item -> modelName == null || modelName.isBlank() || item.getModelName().toLowerCase().contains(modelName.trim().toLowerCase()))
                .filter(item -> promptVersion == null || promptVersion.isBlank() || promptVersion.trim().equals(item.getPromptVersion()))
                .filter(item -> startTime == null || !item.getCreatedAt().isBefore(startTime))
                .filter(item -> endTime == null || !item.getCreatedAt().isAfter(endTime))
                .toList();
        return new PageResult<>(filtered.size(), pageNo, pageSize, paginate(filtered, pageNo, pageSize));
    }

    private <T> List<T> paginate(List<T> source, int pageNo, int pageSize) {
        int fromIndex = Math.min((pageNo - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return source.subList(fromIndex, toIndex);
    }
}

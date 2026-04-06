package com.graduation.repair.service.impl;

import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.AdminCategoryCorrectRequest;
import com.graduation.repair.domain.dto.AdminRollbackRequest;
import com.graduation.repair.domain.entity.OperationLog;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.AdminTicketListItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;
import com.graduation.repair.repository.FaultCategoryRepository;
import com.graduation.repair.repository.NotificationMessageRepository;
import com.graduation.repair.repository.DispatchRecordRepository;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.AdminTicketService;
import com.graduation.repair.service.support.TicketStateMachine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AdminTicketServiceImpl implements AdminTicketService {

    private static final Set<String> ADMIN_ROLLBACK_TARGET = Set.of("已解析", "已派单");

    private final RepairTicketRepository repairTicketRepository;
    private final FaultCategoryRepository faultCategoryRepository;
    private final OperationLogRepository operationLogRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final NotificationMessageRepository notificationMessageRepository;
    private final MaintenanceWorkerRepository maintenanceWorkerRepository;
    private final TicketStateMachine ticketStateMachine;

    public AdminTicketServiceImpl(RepairTicketRepository repairTicketRepository,
                                  FaultCategoryRepository faultCategoryRepository,
                                  OperationLogRepository operationLogRepository,
                                  DispatchRecordRepository dispatchRecordRepository,
                                  NotificationMessageRepository notificationMessageRepository,
                                  MaintenanceWorkerRepository maintenanceWorkerRepository,
                                  TicketStateMachine ticketStateMachine) {
        this.repairTicketRepository = repairTicketRepository;
        this.faultCategoryRepository = faultCategoryRepository;
        this.operationLogRepository = operationLogRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.notificationMessageRepository = notificationMessageRepository;
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
        this.ticketStateMachine = ticketStateMachine;
    }

    @Override
    public PageResult<AdminTicketListItemVO> listTickets(Integer page, Integer size, String status, Long categoryId) {
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);

        Page<RepairTicket> ticketPage;
        if (status != null && !status.isBlank() && categoryId != null) {
            ticketPage = repairTicketRepository.findByStatusAndCategoryIdOrderBySubmittedAtDesc(status.trim(), categoryId, pageable);
        } else if (status != null && !status.isBlank()) {
            ticketPage = repairTicketRepository.findByStatusOrderBySubmittedAtDesc(status.trim(), pageable);
        } else if (categoryId != null) {
            ticketPage = repairTicketRepository.findByCategoryIdOrderBySubmittedAtDesc(categoryId, pageable);
        } else {
            ticketPage = repairTicketRepository.findAllByOrderBySubmittedAtDesc(pageable);
        }

        List<AdminTicketListItemVO> records = ticketPage.getContent().stream().map(item ->
                AdminTicketListItemVO.builder()
                        .ticketId(item.getId())
                        .ticketNo(item.getTicketNo())
                        .reporterId(item.getReporterId())
                        .categoryId(item.getCategoryId())
                        .status(item.getStatus())
                        .currentWorkerId(item.getCurrentWorkerId())
                        .locationText(item.getLocationText())
                        .faultDesc(item.getFaultDesc())
                        .urgencyLevel(item.getUrgencyLevel())
                        .submittedAt(item.getSubmittedAt())
                        .build()).toList();

        return new PageResult<>(ticketPage.getTotalElements(), pageNo, pageSize, records);
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse rollbackTicket(Long operatorId, Long ticketId, AdminRollbackRequest request) {
        RepairTicket ticket = requireTicket(ticketId);
        String targetStatus = request.getTargetStatus().trim();

        if (!ADMIN_ROLLBACK_TARGET.contains(targetStatus)) {
            throw new BizException(4001, "管理员回退目标状态仅支持 已解析 或 已派单");
        }

        if (!ticketStateMachine.canTransit(ticket.getStatus(), targetStatus)) {
            throw new BizException(4001, "非法状态流转：当前状态不允许回退到目标状态");
        }

        String fromStatus = ticket.getStatus();
        ticket.setStatus(targetStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);

        writeLog(ticketId, operatorId, "STATUS_CHANGE", fromStatus, targetStatus,
                "管理员状态回退: " + request.getReason().trim());

        return new TicketStatusChangeResponse(ticketId, fromStatus, targetStatus);
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse correctCategory(Long operatorId, Long ticketId, AdminCategoryCorrectRequest request) {
        RepairTicket ticket = requireTicket(ticketId);

        if (!faultCategoryRepository.existsById(request.getCategoryId())) {
            throw new BizException(4043, "目标分类不存在");
        }

        Long oldCategoryId = ticket.getCategoryId();
        ticket.setCategoryId(request.getCategoryId());
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);

        writeLog(ticketId, operatorId, "CATEGORY_CORRECT", ticket.getStatus(), ticket.getStatus(),
                "管理员修正分类: from=" + oldCategoryId + ", to=" + request.getCategoryId()
                        + ", reason=" + request.getReason().trim());

        return new TicketStatusChangeResponse(ticketId, ticket.getStatus(), ticket.getStatus());
    }

    @Override
    @Transactional
    public void deleteTicket(Long operatorId, Long ticketId) {
        RepairTicket ticket = requireTicket(ticketId);

        if (ticket.getCurrentWorkerId() != null) {
            maintenanceWorkerRepository.findById(ticket.getCurrentWorkerId()).ifPresent(worker -> {
                int current = worker.getCurrentLoad() == null ? 0 : worker.getCurrentLoad();
                worker.setCurrentLoad(Math.max(0, current - 1));
                worker.setUpdatedAt(LocalDateTime.now());
                maintenanceWorkerRepository.save(worker);
            });
        }

        dispatchRecordRepository.deleteByTicketId(ticketId);
        notificationMessageRepository.deleteByTicketId(ticketId);
        operationLogRepository.deleteByTicketId(ticketId);
        repairTicketRepository.delete(ticket);
    }

    private RepairTicket requireTicket(Long ticketId) {
        return repairTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BizException(4041, "工单不存在"));
    }

    private void writeLog(Long ticketId, Long operatorId, String action, String fromStatus, String toStatus, String detail) {
        OperationLog log = new OperationLog();
        log.setTicketId(ticketId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
    }
}

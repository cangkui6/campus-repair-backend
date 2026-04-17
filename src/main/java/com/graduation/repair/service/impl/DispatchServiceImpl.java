package com.graduation.repair.service.impl;

import com.graduation.repair.common.enums.TicketStatus;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.DispatchManualRequest;
import com.graduation.repair.domain.dto.DispatchRetryRequest;
import com.graduation.repair.domain.entity.DispatchRecord;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.OperationLog;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.DispatchResultVO;
import com.graduation.repair.domain.vo.DispatchScoreVO;
import com.graduation.repair.repository.DispatchRecordRepository;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.repository.SysUserRepository;
import com.graduation.repair.service.DispatchService;
import com.graduation.repair.service.NotificationService;
import com.graduation.repair.service.support.DispatchScoreEngine;
import com.graduation.repair.service.support.DispatchWeightManager;
import com.graduation.repair.service.support.TicketStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DispatchServiceImpl implements DispatchService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final RepairTicketRepository repairTicketRepository;
    private final MaintenanceWorkerRepository maintenanceWorkerRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final OperationLogRepository operationLogRepository;
    private final DispatchScoreEngine dispatchScoreEngine;
    private final DispatchWeightManager dispatchWeightManager;
    private final TicketStateMachine ticketStateMachine;
    private final NotificationService notificationService;
    private final SysUserRepository sysUserRepository;

    public DispatchServiceImpl(RepairTicketRepository repairTicketRepository,
                               MaintenanceWorkerRepository maintenanceWorkerRepository,
                               DispatchRecordRepository dispatchRecordRepository,
                               OperationLogRepository operationLogRepository,
                               DispatchScoreEngine dispatchScoreEngine,
                               DispatchWeightManager dispatchWeightManager,
                               TicketStateMachine ticketStateMachine,
                               NotificationService notificationService,
                               SysUserRepository sysUserRepository) {
        this.repairTicketRepository = repairTicketRepository;
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.operationLogRepository = operationLogRepository;
        this.dispatchScoreEngine = dispatchScoreEngine;
        this.dispatchWeightManager = dispatchWeightManager;
        this.ticketStateMachine = ticketStateMachine;
        this.notificationService = notificationService;
        this.sysUserRepository = sysUserRepository;
    }

    @Override
    @Transactional
    public DispatchResultVO autoDispatch(Long operatorId, String role, Long ticketId) {
        ensureAdmin(role);
        RepairTicket ticket = requireTicket(ticketId);
        ensureCanAutoDispatch(ticket);
        String oldStatus = ticket.getStatus();

        List<MaintenanceWorker> candidates = maintenanceWorkerRepository.findByIsAvailable(1);
        if (candidates.isEmpty()) {
            clearAssignmentToPendingManual(ticket, operatorId, "无候选人员，进入待分配池");
            return DispatchResultVO.builder()
                    .ticketId(ticketId)
                    .selectedWorkerId(null)
                    .dispatchStatus("PENDING_MANUAL")
                    .reason("无可用候选维修人员，已进入待分配池")
                    .scores(List.of())
                    .build();
        }

        List<DispatchScoreVO> ranking = dispatchScoreEngine.rank(ticket, candidates);
        DispatchScoreVO best = ranking.get(0);

        updateTicketAssignment(ticket, best.getWorkerId(), TicketStatus.DISPATCHED.getValue());
        saveDispatchRecord(ticketId, best, "AUTO", "ASSIGNED", "自动派单");
        writeDispatchLog(ticketId, operatorId, "DISPATCH", oldStatus, TicketStatus.DISPATCHED.getValue(), "自动派单成功");
        notifyDispatch(ticket, best.getWorkerId(), "系统自动派单");

        return DispatchResultVO.builder()
                .ticketId(ticketId)
                .selectedWorkerId(best.getWorkerId())
                .dispatchStatus("SUCCESS")
                .reason("自动派单成功")
                .scores(ranking)
                .build();
    }

    @Override
    @Transactional
    public DispatchResultVO manualDispatch(Long operatorId, String role, Long ticketId, DispatchManualRequest request) {
        ensureAdmin(role);
        RepairTicket ticket = requireTicket(ticketId);

        MaintenanceWorker worker = maintenanceWorkerRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new BizException(4042, "维修人员不存在"));

        String currentStatus = ticket.getStatus();
        if (!TicketStatus.PARSED.getValue().equals(currentStatus)
                && !TicketStatus.PENDING_DISPATCH.getValue().equals(currentStatus)
                && !TicketStatus.DISPATCHED.getValue().equals(currentStatus)) {
            throw new BizException(4001, "当前状态不支持人工改派");
        }

        updateTicketAssignment(ticket, worker.getId(), TicketStatus.DISPATCHED.getValue());

        DispatchScoreVO score = DispatchScoreVO.builder()
                .workerId(worker.getId())
                .scoreSkill(0.0)
                .scoreArea(0.0)
                .scoreLoad(0.0)
                .scorePerf(0.0)
                .scoreUrgency(0.0)
                .totalScore(0.0)
                .scoreVersion(dispatchWeightManager.activeConfig().getVersionNo())
                .build();

        saveDispatchRecord(ticketId, score, "MANUAL", "ASSIGNED", request.getReason());
        writeDispatchLog(ticketId, operatorId, "REASSIGN", currentStatus, "已派单", "管理员人工改派");
        notifyDispatch(ticket, worker.getId(), "管理员人工改派");

        return DispatchResultVO.builder()
                .ticketId(ticketId)
                .selectedWorkerId(worker.getId())
                .dispatchStatus("SUCCESS")
                .reason("人工改派成功")
                .scores(List.of(score))
                .build();
    }

    @Override
    @Transactional
    public DispatchResultVO retryDispatch(Long operatorId, String role, Long ticketId, DispatchRetryRequest request) {
        ensureAdmin(role);
        RepairTicket ticket = requireTicket(ticketId);
        if (!TicketStatus.DISPATCHED.getValue().equals(ticket.getStatus())) {
            throw new BizException(4001, "仅已派单状态支持拒单重派");
        }

        Long oldWorkerId = ticket.getCurrentWorkerId();
        List<MaintenanceWorker> candidates = maintenanceWorkerRepository.findByIsAvailable(1)
                .stream().filter(w -> !Objects.equals(w.getId(), request.getRejectingWorkerId())).toList();

        if (candidates.isEmpty()) {
            clearAssignmentToPendingManual(ticket, operatorId, "拒单后无候选人员，转待分配池");
            return DispatchResultVO.builder()
                    .ticketId(ticketId)
                    .selectedWorkerId(null)
                    .dispatchStatus("PENDING_MANUAL")
                    .reason("拒单后无可用候选，需人工处理")
                    .scores(List.of())
                    .build();
        }

        List<DispatchScoreVO> ranking = dispatchScoreEngine.rank(ticket, candidates);
        DispatchScoreVO best = ranking.get(0);

        updateTicketAssignment(ticket, best.getWorkerId(), TicketStatus.DISPATCHED.getValue());
        saveDispatchRecord(ticketId, best, "AUTO", "ASSIGNED", "拒单后重派");
        writeDispatchLog(ticketId, operatorId, "DISPATCH", TicketStatus.DISPATCHED.getValue(), TicketStatus.DISPATCHED.getValue(), "拒单重派成功");
        notifyDispatch(ticket, best.getWorkerId(), "拒单后重派成功");

        if (oldWorkerId != null && !Objects.equals(oldWorkerId, best.getWorkerId())) {
            decrementWorkerLoad(oldWorkerId);
        }

        return DispatchResultVO.builder()
                .ticketId(ticketId)
                .selectedWorkerId(best.getWorkerId())
                .dispatchStatus("SUCCESS")
                .reason("拒单重派成功")
                .scores(ranking)
                .build();
    }

    private RepairTicket requireTicket(Long ticketId) {
        return repairTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BizException(4041, "工单不存在"));
    }

    private void ensureCanAutoDispatch(RepairTicket ticket) {
        if (!ticketStateMachine.canTransit(ticket.getStatus(), TicketStatus.DISPATCHED.getValue())) {
            throw new BizException(4001, "自动派单仅允许已解析或待分配工单执行");
        }
    }

    private void updateTicketAssignment(RepairTicket ticket, Long workerId, String status) {
        Long oldWorkerId = ticket.getCurrentWorkerId();
        ticket.setCurrentWorkerId(workerId);
        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);

        if (oldWorkerId != null && !Objects.equals(oldWorkerId, workerId)) {
            decrementWorkerLoad(oldWorkerId);
        }

        maintenanceWorkerRepository.findById(workerId).ifPresent(worker -> {
            int current = worker.getCurrentLoad() == null ? 0 : worker.getCurrentLoad();
            worker.setCurrentLoad(current + 1);
            worker.setUpdatedAt(LocalDateTime.now());
            maintenanceWorkerRepository.save(worker);
        });
    }

    private void decrementWorkerLoad(Long workerId) {
        maintenanceWorkerRepository.findById(workerId).ifPresent(worker -> {
            int current = worker.getCurrentLoad() == null ? 0 : worker.getCurrentLoad();
            worker.setCurrentLoad(Math.max(0, current - 1));
            worker.setUpdatedAt(LocalDateTime.now());
            maintenanceWorkerRepository.save(worker);
        });
    }

    private void clearAssignmentToPendingManual(RepairTicket ticket, Long operatorId, String detail) {
        Long oldWorkerId = ticket.getCurrentWorkerId();
        String oldStatus = ticket.getStatus();
        ticket.setCurrentWorkerId(null);
        ticket.setStatus(TicketStatus.PENDING_DISPATCH.getValue());
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);
        if (oldWorkerId != null) {
            decrementWorkerLoad(oldWorkerId);
        }
        writeDispatchLog(ticket.getId(), operatorId, "DISPATCH", oldStatus, TicketStatus.PENDING_DISPATCH.getValue(), detail + "（PENDING_MANUAL）");
        notifyAdminsPendingManual(ticket, detail);
    }

    private void saveDispatchRecord(Long ticketId, DispatchScoreVO score, String type, String status, String remark) {
        DispatchRecord record = new DispatchRecord();
        record.setTicketId(ticketId);
        record.setWorkerId(score.getWorkerId());
        record.setScoreSkill(decimal(score.getScoreSkill()));
        record.setScoreArea(decimal(score.getScoreArea()));
        record.setScoreLoad(decimal(score.getScoreLoad()));
        record.setScorePerf(decimal(score.getScorePerf()));
        record.setScoreUrgency(decimal(score.getScoreUrgency()));
        record.setTotalScore(decimal(score.getTotalScore()));
        record.setScoreVersion(score.getScoreVersion());
        record.setDispatchType(type);
        record.setDispatchStatus(status);
        record.setRemark(remark);
        record.setCreatedAt(LocalDateTime.now());
        dispatchRecordRepository.save(record);
    }

    private void writeDispatchLog(Long ticketId, Long operatorId, String action, String fromStatus, String toStatus, String detail) {
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

    private BigDecimal decimal(Double value) {
        return BigDecimal.valueOf(value == null ? 0.0 : value);
    }

    private void notifyDispatch(RepairTicket ticket, Long workerId, String scene) {
        maintenanceWorkerRepository.findById(workerId).ifPresent(worker -> {
            notificationService.notifyUser(worker.getUserId(), ticket.getId(), "收到新的派单", scene + "，工单号：" + ticket.getTicketNo());
        });
        notificationService.notifyUser(ticket.getReporterId(), ticket.getId(), "工单已派单", "工单" + ticket.getTicketNo() + "已派单，请耐心等待处理");
    }

    private void notifyAdminsPendingManual(RepairTicket ticket, String reason) {
        sysUserRepository.findByRole(ROLE_ADMIN)
                .forEach(admin -> notificationService.notifyUser(admin.getId(), ticket.getId(), "工单待人工分配", "工单" + ticket.getTicketNo() + "进入待分配池，原因：" + reason));
    }

    private void ensureAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            throw new BizException(4036, "仅管理员可执行派单操作");
        }
    }
}

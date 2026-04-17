package com.graduation.repair.service.impl;

import com.graduation.repair.common.enums.TicketStatus;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.entity.DispatchRecord;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.entity.SysUser;
import com.graduation.repair.domain.vo.WorkerHistoryItemVO;
import com.graduation.repair.domain.vo.WorkerProfileVO;
import com.graduation.repair.domain.vo.WorkerTaskListItemVO;
import com.graduation.repair.repository.DispatchRecordRepository;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.repository.SysUserRepository;
import com.graduation.repair.service.NotificationService;
import com.graduation.repair.service.WorkerTaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkerTaskServiceImpl implements WorkerTaskService {

    private final MaintenanceWorkerRepository maintenanceWorkerRepository;
    private final RepairTicketRepository repairTicketRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final SysUserRepository sysUserRepository;
    private final NotificationService notificationService;

    public WorkerTaskServiceImpl(MaintenanceWorkerRepository maintenanceWorkerRepository,
                                 RepairTicketRepository repairTicketRepository,
                                 DispatchRecordRepository dispatchRecordRepository,
                                 SysUserRepository sysUserRepository,
                                 NotificationService notificationService) {
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
        this.repairTicketRepository = repairTicketRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.sysUserRepository = sysUserRepository;
        this.notificationService = notificationService;
    }

    @Override
    public PageResult<WorkerTaskListItemVO> myTasks(Long userId, String role, Integer page, Integer size, String status) {
        ensureWorker(role);
        MaintenanceWorker worker = requireWorker(userId);
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size));
        Page<RepairTicket> ticketPage = (status == null || status.isBlank())
                ? repairTicketRepository.findByCurrentWorkerIdOrderBySubmittedAtDesc(worker.getId(), pageable)
                : repairTicketRepository.findByCurrentWorkerIdAndStatusOrderBySubmittedAtDesc(worker.getId(), status.trim(), pageable);
        return new PageResult<>(ticketPage.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize(),
                ticketPage.getContent().stream().map(this::toTaskItem).toList());
    }

    @Override
    public PageResult<WorkerTaskListItemVO> availableTasks(Long userId, String role, Integer page, Integer size) {
        ensureWorker(role);
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size));
        Page<RepairTicket> ticketPage = repairTicketRepository.findByStatusAndCurrentWorkerIdIsNullOrderBySubmittedAtDesc(TicketStatus.PENDING_DISPATCH.getValue(), pageable);
        return new PageResult<>(ticketPage.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize(),
                ticketPage.getContent().stream().map(this::toTaskItem).toList());
    }

    @Override
    public PageResult<WorkerHistoryItemVO> myHistory(Long userId, String role, Integer page, Integer size) {
        ensureWorker(role);
        MaintenanceWorker worker = requireWorker(userId);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        List<WorkerHistoryItemVO> allRecords = repairTicketRepository.findByCurrentWorkerIdOrderBySubmittedAtDesc(worker.getId(), Pageable.unpaged()).getContent().stream()
                .filter(item -> TicketStatus.COMPLETED.getValue().equals(item.getStatus()) || TicketStatus.EVALUATED.getValue().equals(item.getStatus()) || TicketStatus.CLOSED.getValue().equals(item.getStatus()))
                .map(item -> WorkerHistoryItemVO.builder()
                        .ticketId(item.getId())
                        .ticketNo(item.getTicketNo())
                        .status(item.getStatus())
                        .locationText(item.getLocationText())
                        .faultDesc(item.getFaultDesc())
                        .submittedAt(item.getSubmittedAt())
                        .completedAt(item.getCompletedAt())
                        .build())
                .toList();
        return new PageResult<>(allRecords.size(), pageNo, pageSize, paginate(allRecords, pageNo, pageSize));
    }

    @Override
    public WorkerProfileVO myProfile(Long userId, String role) {
        ensureWorker(role);
        MaintenanceWorker worker = requireWorker(userId);
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        long processingCount = repairTicketRepository.findByCurrentWorkerIdOrderBySubmittedAtDesc(worker.getId(), PageRequest.of(0, 100)).getContent().stream()
                .filter(item -> TicketStatus.DISPATCHED.getValue().equals(item.getStatus()) || TicketStatus.PROCESSING.getValue().equals(item.getStatus()))
                .count();
        long historyCount = repairTicketRepository.findByCurrentWorkerIdOrderBySubmittedAtDesc(worker.getId(), PageRequest.of(0, 100)).getContent().stream()
                .filter(item -> TicketStatus.COMPLETED.getValue().equals(item.getStatus()) || TicketStatus.EVALUATED.getValue().equals(item.getStatus()) || TicketStatus.CLOSED.getValue().equals(item.getStatus()))
                .count();
        return WorkerProfileVO.builder()
                .workerId(worker.getId())
                .workerName(user == null ? ("维修人员-" + worker.getId()) : user.getRealName())
                .skillTags(worker.getSkillTags())
                .serviceArea(worker.getServiceArea())
                .currentLoad(worker.getCurrentLoad())
                .avgCompleteHours(worker.getAvgCompleteHours())
                .acceptRate(worker.getAcceptRate())
                .completedTicketCount(worker.getCompletedTicketCount())
                .reassignCount(worker.getReassignCount())
                .lastActiveAt(worker.getLastActiveAt())
                .processingCount(processingCount)
                .historyCount(historyCount)
                .build();
    }

    @Override
    @Transactional
    public void grabTask(Long userId, String role, Long ticketId) {
        ensureWorker(role);
        MaintenanceWorker worker = requireWorker(userId);
        RepairTicket ticket = requireTicket(ticketId);
        if (!TicketStatus.PENDING_DISPATCH.getValue().equals(ticket.getStatus()) || ticket.getCurrentWorkerId() != null) {
            throw new BizException(4001, "当前工单不可抢单");
        }
        ticket.setCurrentWorkerId(worker.getId());
        ticket.setStatus(TicketStatus.DISPATCHED.getValue());
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);
        worker.setCurrentLoad((worker.getCurrentLoad() == null ? 0 : worker.getCurrentLoad()) + 1);
        worker.setUpdatedAt(LocalDateTime.now());
        maintenanceWorkerRepository.save(worker);
        notificationService.notifyUser(ticket.getReporterId(), ticket.getId(), "工单已被抢单", "工单" + ticket.getTicketNo() + "已有维修人员接单处理中");
    }

    @Override
    @Transactional
    public void rejectTask(Long userId, String role, Long ticketId, String reason) {
        ensureWorker(role);
        MaintenanceWorker worker = requireWorker(userId);
        RepairTicket ticket = requireTicket(ticketId);
        if (!worker.getId().equals(ticket.getCurrentWorkerId()) || !TicketStatus.DISPATCHED.getValue().equals(ticket.getStatus())) {
            throw new BizException(4001, "当前工单不可拒单");
        }
        ticket.setCurrentWorkerId(null);
        ticket.setStatus(TicketStatus.PENDING_DISPATCH.getValue());
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);
        worker.setCurrentLoad(Math.max(0, (worker.getCurrentLoad() == null ? 0 : worker.getCurrentLoad()) - 1));
        worker.setUpdatedAt(LocalDateTime.now());
        maintenanceWorkerRepository.save(worker);

        DispatchRecord record = new DispatchRecord();
        record.setTicketId(ticket.getId());
        record.setWorkerId(worker.getId());
        record.setDispatchType("MANUAL");
        record.setDispatchStatus("REJECTED");
        record.setRemark(reason == null || reason.isBlank() ? "维修人员拒单" : "维修人员拒单: " + reason.trim());
        record.setCreatedAt(LocalDateTime.now());
        dispatchRecordRepository.save(record);
        notificationService.notifyUser(ticket.getReporterId(), ticket.getId(), "工单重新进入待分配", "工单" + ticket.getTicketNo() + "因维修人员拒单已重新进入待分配池");
    }

    private WorkerTaskListItemVO toTaskItem(RepairTicket item) {
        return WorkerTaskListItemVO.builder()
                .ticketId(item.getId())
                .ticketNo(item.getTicketNo())
                .status(item.getStatus())
                .locationText(item.getLocationText())
                .faultDesc(item.getFaultDesc())
                .urgencyLevel(item.getUrgencyLevel())
                .submittedAt(item.getSubmittedAt())
                .build();
    }

    private MaintenanceWorker requireWorker(Long userId) {
        return maintenanceWorkerRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(4042, "维修人员档案不存在"));
    }

    private RepairTicket requireTicket(Long ticketId) {
        return repairTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BizException(4041, "工单不存在"));
    }

    private void ensureWorker(String role) {
        if (!"WORKER".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(4035, "仅维修人员或管理员可执行该操作");
        }
    }

    private int normalizePage(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return (size == null || size < 1) ? 10 : Math.min(size, 50);
    }

    private <T> List<T> paginate(List<T> source, int pageNo, int pageSize) {
        int fromIndex = Math.min((pageNo - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return source.subList(fromIndex, toIndex);
    }
}

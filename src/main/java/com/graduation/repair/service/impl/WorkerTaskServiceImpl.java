package com.graduation.repair.service.impl;

import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.WorkerTaskListItemVO;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.WorkerTaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkerTaskServiceImpl implements WorkerTaskService {

    private final MaintenanceWorkerRepository maintenanceWorkerRepository;
    private final RepairTicketRepository repairTicketRepository;

    public WorkerTaskServiceImpl(MaintenanceWorkerRepository maintenanceWorkerRepository,
                                 RepairTicketRepository repairTicketRepository) {
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
        this.repairTicketRepository = repairTicketRepository;
    }

    @Override
    public PageResult<WorkerTaskListItemVO> myTasks(Long userId, String role, Integer page, Integer size, String status) {
        if (!"WORKER".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(4035, "仅维修人员或管理员可查看维修任务");
        }

        MaintenanceWorker worker = maintenanceWorkerRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(4042, "维修人员档案不存在"));

        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);

        Page<RepairTicket> ticketPage;
        if (status == null || status.isBlank()) {
            ticketPage = repairTicketRepository.findByCurrentWorkerIdOrderBySubmittedAtDesc(worker.getId(), pageable);
        } else {
            ticketPage = repairTicketRepository.findByCurrentWorkerIdAndStatusOrderBySubmittedAtDesc(worker.getId(), status.trim(), pageable);
        }

        List<WorkerTaskListItemVO> records = ticketPage.getContent().stream().map(item -> WorkerTaskListItemVO.builder()
                .ticketId(item.getId())
                .ticketNo(item.getTicketNo())
                .status(item.getStatus())
                .locationText(item.getLocationText())
                .faultDesc(item.getFaultDesc())
                .urgencyLevel(item.getUrgencyLevel())
                .submittedAt(item.getSubmittedAt())
                .build()).toList();

        return new PageResult<>(ticketPage.getTotalElements(), pageNo, pageSize, records);
    }
}

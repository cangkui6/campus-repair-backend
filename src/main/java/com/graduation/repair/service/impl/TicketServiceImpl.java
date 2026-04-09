package com.graduation.repair.service.impl;

import com.graduation.repair.common.enums.TicketStatus;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.TicketCompleteRequest;
import com.graduation.repair.domain.dto.TicketCreateRequest;
import com.graduation.repair.domain.dto.TicketEvaluateRequest;
import com.graduation.repair.domain.dto.TicketSupplementRequest;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.OperationLog;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.ReporterEvaluationItemVO;
import com.graduation.repair.domain.vo.TicketCreateResponse;
import com.graduation.repair.domain.vo.TicketDetailVO;
import com.graduation.repair.domain.vo.TicketMyListItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.DispatchFeedbackService;
import com.graduation.repair.service.NotificationService;
import com.graduation.repair.service.TicketService;
import com.graduation.repair.service.support.TicketNoGenerator;
import com.graduation.repair.service.support.TicketStateMachine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {

    private static final String ROLE_REPORTER = "REPORTER";
    private static final String ROLE_WORKER = "WORKER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final RepairTicketRepository repairTicketRepository;
    private final OperationLogRepository operationLogRepository;
    private final TicketNoGenerator ticketNoGenerator;
    private final TicketStateMachine ticketStateMachine;
    private final DispatchFeedbackService dispatchFeedbackService;
    private final NotificationService notificationService;
    private final MaintenanceWorkerRepository maintenanceWorkerRepository;

    public TicketServiceImpl(RepairTicketRepository repairTicketRepository,
                             OperationLogRepository operationLogRepository,
                             TicketNoGenerator ticketNoGenerator,
                             TicketStateMachine ticketStateMachine,
                             DispatchFeedbackService dispatchFeedbackService,
                             NotificationService notificationService,
                             MaintenanceWorkerRepository maintenanceWorkerRepository) {
        this.repairTicketRepository = repairTicketRepository;
        this.operationLogRepository = operationLogRepository;
        this.ticketNoGenerator = ticketNoGenerator;
        this.ticketStateMachine = ticketStateMachine;
        this.dispatchFeedbackService = dispatchFeedbackService;
        this.notificationService = notificationService;
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
    }

    @Override
    @Transactional
    public TicketCreateResponse createTicket(Long reporterId, String role, TicketCreateRequest request) {
        ensureReporter(role);
        String rawTextInput = request.getRawText();
        if (rawTextInput == null || rawTextInput.isBlank()) {
            throw new BizException(4002, "报修内容不能为空");
        }
        String rawText = rawTextInput.trim();
        if (rawText.length() < 2) {
            throw new BizException(4002, "报修内容过短，请补充故障信息");
        }
        if (rawText.length() > 1000) {
            throw new BizException(4003, "报修内容过长，请精简后重试");
        }

        LocalDateTime now = LocalDateTime.now();

        RepairTicket ticket = new RepairTicket();
        ticket.setTicketNo(ticketNoGenerator.nextNo());
        ticket.setReporterId(reporterId);
        ticket.setRawText(rawText);
        ticket.setContactMasked(maskPhone(request.getContactPhone()));
        ticket.setStatus(TicketStatus.PENDING.getValue());
        ticket.setSubmittedAt(now);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        RepairTicket saved = repairTicketRepository.save(ticket);

        writeStatusLog(saved.getId(), reporterId, "CREATE", null, TicketStatus.PENDING.getValue(), "用户提交自然语言报修");

        return new TicketCreateResponse(saved.getId(), saved.getTicketNo(), saved.getStatus());
    }

    @Override
    public TicketDetailVO getTicketDetail(Long userId, String role, Long ticketId) {
        RepairTicket ticket = requireTicket(ticketId);

        if (ROLE_REPORTER.equals(role) && !ticket.getReporterId().equals(userId)) {
            throw new BizException(4031, "无权限访问该工单");
        }

        return TicketDetailVO.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .rawText(ticket.getRawText())
                .locationText(ticket.getLocationText())
                .faultDesc(ticket.getFaultDesc())
                .urgencyLevel(ticket.getUrgencyLevel())
                .categoryId(ticket.getCategoryId())
                .status(ticket.getStatus())
                .contactMasked(ticket.getContactMasked())
                .submittedAt(ticket.getSubmittedAt())
                .build();
    }

    @Override
    public PageResult<TicketMyListItemVO> myTickets(Long reporterId, String role, Integer page, Integer size, String status) {
        ensureReporter(role);

        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);

        Page<RepairTicket> ticketPage;
        if (status == null || status.isBlank()) {
            ticketPage = repairTicketRepository.findByReporterIdOrderBySubmittedAtDesc(reporterId, pageable);
        } else {
            ticketPage = repairTicketRepository.findByReporterIdAndStatusOrderBySubmittedAtDesc(reporterId, status.trim(), pageable);
        }

        List<TicketMyListItemVO> records = ticketPage.getContent().stream().map(item ->
                TicketMyListItemVO.builder()
                        .ticketId(item.getId())
                        .ticketNo(item.getTicketNo())
                        .status(item.getStatus())
                        .faultDesc(item.getFaultDesc())
                        .locationText(item.getLocationText())
                        .submittedAt(item.getSubmittedAt())
                        .build()
        ).toList();

        return new PageResult<>(ticketPage.getTotalElements(), pageNo, pageSize, records);
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse supplementTicket(Long operatorId, String role, Long ticketId, TicketSupplementRequest request) {
        ensureReporter(role);
        RepairTicket ticket = requireTicket(ticketId);
        if (!ticket.getReporterId().equals(operatorId)) {
            throw new BizException(4034, "仅报修人可补充信息");
        }
        String supplementText = request.getSupplementText() == null ? "" : request.getSupplementText().trim();
        if (supplementText.isBlank()) {
            throw new BizException(4002, "补充信息不能为空");
        }
        ticket.setRawText(ticket.getRawText() + "\n【补充信息】" + supplementText);
        if (request.getContactPhone() != null && !request.getContactPhone().isBlank()) {
            ticket.setContactMasked(maskPhone(request.getContactPhone()));
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);
        writeStatusLog(ticket.getId(), operatorId, "SUPPLEMENT", ticket.getStatus(), ticket.getStatus(), "用户补充信息: " + supplementText);
        notificationService.notifyUser(operatorId, ticket.getId(), "补充信息已提交", "工单" + ticket.getTicketNo() + "补充信息已保存");
        return new TicketStatusChangeResponse(ticket.getId(), ticket.getStatus(), ticket.getStatus());
    }

    @Override
    public PageResult<ReporterEvaluationItemVO> myEvaluations(Long reporterId, String role, Integer page, Integer size) {
        ensureReporter(role);
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        Page<RepairTicket> ticketPage = repairTicketRepository.findByReporterIdOrderBySubmittedAtDesc(reporterId, pageable);
        Map<Long, List<OperationLog>> logMap = operationLogRepository.findAll().stream()
                .filter(item -> item.getDetail() != null && item.getDetail().contains("用户评价:"))
                .collect(Collectors.groupingBy(OperationLog::getTicketId));

        List<ReporterEvaluationItemVO> records = ticketPage.getContent().stream()
                .map(ticket -> toEvaluation(ticket, logMap.get(ticket.getId())))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageResult<>(records.size(), pageNo, pageSize, records);
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse acceptTicket(Long operatorId, String role, Long ticketId) {
        ensureWorkerOrAdmin(role);
        RepairTicket ticket = requireTicket(ticketId);

        if (ROLE_WORKER.equals(role)) {
            Long workerRecordId = requireWorkerRecordId(operatorId);
            if (ticket.getCurrentWorkerId() == null || !ticket.getCurrentWorkerId().equals(workerRecordId)) {
                throw new BizException(4032, "仅派单给当前维修人员的工单可接单");
            }
        }

        return changeStatus(ticket, operatorId, TicketStatus.PROCESSING.getValue(), "接单并开始处理");
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse completeTicket(Long operatorId, String role, Long ticketId, TicketCompleteRequest request) {
        ensureWorkerOrAdmin(role);
        RepairTicket ticket = requireTicket(ticketId);

        if (ROLE_WORKER.equals(role)) {
            Long workerRecordId = requireWorkerRecordId(operatorId);
            if (ticket.getCurrentWorkerId() == null || !ticket.getCurrentWorkerId().equals(workerRecordId)) {
                throw new BizException(4033, "仅当前维修人员可提交完成");
            }
        }

        TicketStatusChangeResponse response = changeStatus(ticket, operatorId, TicketStatus.COMPLETED.getValue(), "维修完成: " + request.getRepairResult());
        ticket.setCompletedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);
        dispatchFeedbackService.onTicketCompleted(operatorId, ticketId);
        return response;
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse evaluateTicket(Long operatorId, String role, Long ticketId, TicketEvaluateRequest request) {
        ensureReporter(role);
        RepairTicket ticket = requireTicket(ticketId);

        if (!ticket.getReporterId().equals(operatorId)) {
            throw new BizException(4034, "仅报修人可评价工单");
        }

        String detail = "用户评价: score=" + request.getScore();
        if (request.getComment() != null && !request.getComment().isBlank()) {
            detail = detail + ", comment=" + request.getComment().trim();
        }
        return changeStatus(ticket, operatorId, TicketStatus.EVALUATED.getValue(), detail);
    }

    @Override
    @Transactional
    public TicketStatusChangeResponse closeTicket(Long operatorId, String role, Long ticketId) {
        ensureAdmin(role);
        RepairTicket ticket = requireTicket(ticketId);

        TicketStatusChangeResponse response = changeStatus(ticket, operatorId, TicketStatus.CLOSED.getValue(), "管理员归档关闭工单");
        ticket.setClosedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);
        return response;
    }

    private ReporterEvaluationItemVO toEvaluation(RepairTicket ticket, List<OperationLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        OperationLog latest = logs.stream().max(java.util.Comparator.comparing(OperationLog::getCreatedAt)).orElse(null);
        if (latest == null) {
            return null;
        }
        String detail = latest.getDetail() == null ? "" : latest.getDetail();
        return ReporterEvaluationItemVO.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .score(parseScore(detail))
                .comment(parseComment(detail))
                .ticketStatus(ticket.getStatus())
                .evaluatedAt(latest.getCreatedAt())
                .build();
    }

    private Integer parseScore(String detail) {
        try {
            int idx = detail.indexOf("score=");
            if (idx < 0) {
                return null;
            }
            return Integer.valueOf(detail.substring(idx + 6).split(",")[0].trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String parseComment(String detail) {
        int idx = detail.indexOf("comment=");
        return idx < 0 ? "" : detail.substring(idx + 8).trim();
    }

    private TicketStatusChangeResponse changeStatus(RepairTicket ticket, Long operatorId, String targetStatus, String detail) {
        String fromStatus = ticket.getStatus();
        if (!ticketStateMachine.canTransit(fromStatus, targetStatus)) {
            throw new BizException(4001, "非法状态流转：当前状态不允许执行该操作");
        }

        ticket.setStatus(targetStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        repairTicketRepository.save(ticket);

        writeStatusLog(ticket.getId(), operatorId, "STATUS_CHANGE", fromStatus, targetStatus, detail);
        notificationService.notifyUser(ticket.getReporterId(), ticket.getId(), "工单状态更新", "工单" + ticket.getTicketNo() + "状态已更新为" + targetStatus);
        return new TicketStatusChangeResponse(ticket.getId(), fromStatus, targetStatus);
    }

    private Long requireWorkerRecordId(Long userId) {
        MaintenanceWorker worker = maintenanceWorkerRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(4042, "维修人员档案不存在"));
        return worker.getId();
    }

    private RepairTicket requireTicket(Long ticketId) {
        return repairTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BizException(4041, "工单不存在"));
    }

    private void writeStatusLog(Long ticketId, Long operatorId, String action, String fromStatus, String toStatus, String detail) {
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

    private void ensureReporter(String role) {
        if (!ROLE_REPORTER.equals(role)) {
            throw new BizException(4030, "仅报修用户可执行该操作");
        }
    }

    private void ensureWorkerOrAdmin(String role) {
        if (!ROLE_WORKER.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new BizException(4035, "仅维修人员或管理员可执行该操作");
        }
    }

    private void ensureAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            throw new BizException(4036, "仅管理员可执行该操作");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String pure = phone.replaceAll("\\s+", "").trim();
        if (pure.length() < 7) {
            return pure;
        }
        return pure.substring(0, 3) + "****" + pure.substring(pure.length() - 4);
    }
}

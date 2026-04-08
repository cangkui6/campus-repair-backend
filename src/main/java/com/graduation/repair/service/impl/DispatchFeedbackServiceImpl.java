package com.graduation.repair.service.impl;

import com.graduation.repair.domain.dto.DispatchWeightUpdateRequest;
import com.graduation.repair.domain.entity.DispatchFeedbackSnapshot;
import com.graduation.repair.domain.entity.DispatchRecord;
import com.graduation.repair.domain.entity.DispatchWeightConfig;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.DispatchFeedbackOverviewVO;
import com.graduation.repair.domain.vo.DispatchFeedbackSnapshotVO;
import com.graduation.repair.domain.vo.DispatchWeightConfigVO;
import com.graduation.repair.repository.DispatchFeedbackSnapshotRepository;
import com.graduation.repair.repository.DispatchRecordRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.DispatchFeedbackService;
import com.graduation.repair.service.support.DispatchWeightManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DispatchFeedbackServiceImpl implements DispatchFeedbackService {

    private final DispatchRecordRepository dispatchRecordRepository;
    private final DispatchFeedbackSnapshotRepository dispatchFeedbackSnapshotRepository;
    private final RepairTicketRepository repairTicketRepository;
    private final DispatchWeightManager dispatchWeightManager;

    public DispatchFeedbackServiceImpl(DispatchRecordRepository dispatchRecordRepository,
                                       DispatchFeedbackSnapshotRepository dispatchFeedbackSnapshotRepository,
                                       RepairTicketRepository repairTicketRepository,
                                       DispatchWeightManager dispatchWeightManager) {
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.dispatchFeedbackSnapshotRepository = dispatchFeedbackSnapshotRepository;
        this.repairTicketRepository = repairTicketRepository;
        this.dispatchWeightManager = dispatchWeightManager;
    }

    @Override
    public DispatchFeedbackOverviewVO overview() {
        DispatchWeightConfig active = dispatchWeightManager.activeConfig();
        DispatchFeedbackSnapshot latest = dispatchFeedbackSnapshotRepository.findAll().stream()
                .max(Comparator.comparing(DispatchFeedbackSnapshot::getCreatedAt))
                .orElseGet(() -> emptySnapshot(active.getVersionNo()));
        return new DispatchFeedbackOverviewVO(toWeightVO(active), toSnapshotVO(latest));
    }

    @Override
    @Transactional
    public DispatchFeedbackOverviewVO recalculate(Long operatorId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(7);
        List<DispatchRecord> records = dispatchRecordRepository.findByCreatedAtBetween(start, end);
        Map<Long, RepairTicket> ticketMap = repairTicketRepository.findAllById(records.stream().map(DispatchRecord::getTicketId).distinct().toList())
                .stream().collect(Collectors.toMap(RepairTicket::getId, Function.identity()));

        int dispatchCount = records.size();
        long reassignCount = records.stream().filter(item -> "MANUAL".equals(item.getDispatchType())).count();
        long rejectCount = records.stream().filter(item -> item.getRemark() != null && item.getRemark().contains("拒单")).count();
        List<RepairTicket> completed = ticketMap.values().stream().filter(item -> item.getCompletedAt() != null).toList();
        long timeoutCount = completed.stream().filter(item -> Duration.between(item.getSubmittedAt(), item.getCompletedAt()).toHours() > 24).count();
        double avgCompleteHours = completed.isEmpty() ? 0.0 : completed.stream()
                .mapToLong(item -> Duration.between(item.getSubmittedAt(), item.getCompletedAt()).toHours())
                .average().orElse(0.0);

        double reassignRate = ratio(reassignCount, dispatchCount);
        double rejectRate = ratio(rejectCount, dispatchCount);
        double timeoutRate = ratio(timeoutCount, completed.size());

        DispatchWeightConfig newConfig = dispatchWeightManager.activateNext(
                clamp(0.35 - rejectRate * 0.10 + timeoutRate * 0.05),
                clamp(0.20 + timeoutRate * 0.05),
                clamp(0.20 - reassignRate * 0.08),
                clamp(0.10 + rejectRate * 0.05),
                clamp(0.15 + timeoutRate * 0.08),
                operatorId == null ? "SCHEDULED_FEEDBACK" : "ADMIN_MANUAL_RECALCULATE"
        );

        DispatchFeedbackSnapshot snapshot = new DispatchFeedbackSnapshot();
        snapshot.setWindowStart(start);
        snapshot.setWindowEnd(end);
        snapshot.setDispatchCount(dispatchCount);
        snapshot.setReassignRate(decimal(reassignRate));
        snapshot.setRejectRate(decimal(rejectRate));
        snapshot.setTimeoutRate(decimal(timeoutRate));
        snapshot.setAvgCompleteHours(decimal(avgCompleteHours));
        snapshot.setAppliedVersion(newConfig.getVersionNo());
        snapshot.setCreatedAt(LocalDateTime.now());
        dispatchFeedbackSnapshotRepository.save(snapshot);
        return new DispatchFeedbackOverviewVO(toWeightVO(newConfig), toSnapshotVO(snapshot));
    }

    @Override
    @Transactional
    public DispatchFeedbackOverviewVO updateWeights(Long operatorId, DispatchWeightUpdateRequest request) {
        double total = safe(request.getWeightSkill()) + safe(request.getWeightArea()) + safe(request.getWeightLoad()) + safe(request.getWeightPerf()) + safe(request.getWeightUrgency());
        if (Math.abs(total - 1.0) > 0.0001) {
            throw new IllegalArgumentException("权重之和必须为1");
        }
        DispatchWeightConfig config = dispatchWeightManager.activateNext(
                request.getWeightSkill(),
                request.getWeightArea(),
                request.getWeightLoad(),
                request.getWeightPerf(),
                request.getWeightUrgency(),
                operatorId == null ? "ADMIN_RULE_CONFIG" : "ADMIN_RULE_CONFIG"
        );
        return new DispatchFeedbackOverviewVO(toWeightVO(config), null);
    }

    @Override
    public void onTicketCompleted(Long operatorId, Long ticketId) {
        recalculate(operatorId);
    }

    private DispatchWeightConfigVO toWeightVO(DispatchWeightConfig config) {
        return config == null ? null : DispatchWeightConfigVO.builder()
                .versionNo(config.getVersionNo())
                .weightSkill(config.getWeightSkill().doubleValue())
                .weightArea(config.getWeightArea().doubleValue())
                .weightLoad(config.getWeightLoad().doubleValue())
                .weightPerf(config.getWeightPerf().doubleValue())
                .weightUrgency(config.getWeightUrgency().doubleValue())
                .triggerSource(config.getTriggerSource())
                .active(config.getIsActive() == 1)
                .build();
    }

    private DispatchFeedbackSnapshot emptySnapshot(Integer appliedVersion) {
        DispatchFeedbackSnapshot snapshot = new DispatchFeedbackSnapshot();
        snapshot.setWindowStart(LocalDateTime.now().minusDays(7));
        snapshot.setWindowEnd(LocalDateTime.now());
        snapshot.setDispatchCount(0);
        snapshot.setReassignRate(decimal(0));
        snapshot.setRejectRate(decimal(0));
        snapshot.setTimeoutRate(decimal(0));
        snapshot.setAvgCompleteHours(decimal(0));
        snapshot.setAppliedVersion(appliedVersion == null ? 1 : appliedVersion);
        snapshot.setCreatedAt(LocalDateTime.now());
        return snapshot;
    }

    private DispatchFeedbackSnapshotVO toSnapshotVO(DispatchFeedbackSnapshot snapshot) {
        return snapshot == null ? null : DispatchFeedbackSnapshotVO.builder()
                .windowStart(snapshot.getWindowStart())
                .windowEnd(snapshot.getWindowEnd())
                .dispatchCount(snapshot.getDispatchCount())
                .reassignRate(snapshot.getReassignRate().doubleValue())
                .rejectRate(snapshot.getRejectRate().doubleValue())
                .timeoutRate(snapshot.getTimeoutRate().doubleValue())
                .avgCompleteHours(snapshot.getAvgCompleteHours().doubleValue())
                .appliedVersion(snapshot.getAppliedVersion())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }

    private double ratio(long numerator, int denominator) {
        return denominator <= 0 ? 0.0 : round4((double) numerator / denominator);
    }

    private double clamp(double value) {
        return round4(Math.max(0.05, Math.min(0.60, value)));
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(round4(value));
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}

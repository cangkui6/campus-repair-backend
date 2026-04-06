package com.graduation.repair.service.impl;

import com.graduation.repair.domain.entity.DispatchRecord;
import com.graduation.repair.domain.entity.FaultCategory;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.entity.SysUser;
import com.graduation.repair.domain.vo.CategoryStatItemVO;
import com.graduation.repair.domain.vo.EfficiencyStatsVO;
import com.graduation.repair.domain.vo.OverviewStatsVO;
import com.graduation.repair.domain.vo.WorkerLoadStatItemVO;
import com.graduation.repair.repository.DispatchRecordRepository;
import com.graduation.repair.repository.FaultCategoryRepository;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.repository.SysUserRepository;
import com.graduation.repair.service.StatsService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatsServiceImpl implements StatsService {

    private final RepairTicketRepository repairTicketRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final FaultCategoryRepository faultCategoryRepository;
    private final MaintenanceWorkerRepository maintenanceWorkerRepository;
    private final SysUserRepository sysUserRepository;

    public StatsServiceImpl(RepairTicketRepository repairTicketRepository,
                            DispatchRecordRepository dispatchRecordRepository,
                            FaultCategoryRepository faultCategoryRepository,
                            MaintenanceWorkerRepository maintenanceWorkerRepository,
                            SysUserRepository sysUserRepository) {
        this.repairTicketRepository = repairTicketRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.faultCategoryRepository = faultCategoryRepository;
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
        this.sysUserRepository = sysUserRepository;
    }

    @Override
    public OverviewStatsVO overview(LocalDate startDate, LocalDate endDate) {
        List<RepairTicket> tickets = queryTickets(startDate, endDate);
        List<DispatchRecord> records = queryDispatchRecords(startDate, endDate);

        double avgResponseMinutes = calcAvgResponseMinutes(tickets, records);
        double avgCompleteHours = calcAvgCompleteHours(tickets);

        return OverviewStatsVO.builder()
                .ticketCount((long) tickets.size())
                .avgResponseMinutes(avgResponseMinutes)
                .avgCompleteHours(avgCompleteHours)
                .build();
    }

    @Override
    public List<CategoryStatItemVO> categoryDistribution(LocalDate startDate, LocalDate endDate) {
        List<RepairTicket> tickets = queryTickets(startDate, endDate);
        Map<Long, Long> grouped = tickets.stream()
                .filter(t -> t.getCategoryId() != null)
                .collect(Collectors.groupingBy(RepairTicket::getCategoryId, Collectors.counting()));

        Map<Long, FaultCategory> categoryMap = faultCategoryRepository.findAllById(grouped.keySet())
                .stream().collect(Collectors.toMap(FaultCategory::getId, Function.identity()));

        return grouped.entrySet().stream()
                .map(e -> CategoryStatItemVO.builder()
                        .name(categoryMap.containsKey(e.getKey()) ? categoryMap.get(e.getKey()).getCategoryName() : "未知分类")
                        .count(e.getValue())
                        .build())
                .sorted(Comparator.comparing(CategoryStatItemVO::getCount).reversed())
                .toList();
    }

    @Override
    public EfficiencyStatsVO efficiency(LocalDate startDate, LocalDate endDate) {
        List<RepairTicket> tickets = queryTickets(startDate, endDate);
        List<DispatchRecord> records = queryDispatchRecords(startDate, endDate);

        return EfficiencyStatsVO.builder()
                .avgResponseMinutes(calcAvgResponseMinutes(tickets, records))
                .avgCompleteHours(calcAvgCompleteHours(tickets))
                .build();
    }

    @Override
    public List<WorkerLoadStatItemVO> workerLoad(LocalDate startDate, LocalDate endDate) {
        List<DispatchRecord> records = queryDispatchRecords(startDate, endDate);
        Map<Long, Long> grouped = records.stream()
                .collect(Collectors.groupingBy(DispatchRecord::getWorkerId, Collectors.counting()));

        Map<Long, MaintenanceWorker> workerMap = maintenanceWorkerRepository.findAllById(grouped.keySet())
                .stream().collect(Collectors.toMap(MaintenanceWorker::getId, Function.identity()));

        List<Long> userIds = workerMap.values().stream().map(MaintenanceWorker::getUserId).filter(Objects::nonNull).toList();
        Map<Long, SysUser> userMap = sysUserRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<WorkerLoadStatItemVO> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : grouped.entrySet()) {
            MaintenanceWorker worker = workerMap.get(entry.getKey());
            String workerName = "未知维修人员";
            if (worker != null && worker.getUserId() != null && userMap.containsKey(worker.getUserId())) {
                workerName = userMap.get(worker.getUserId()).getRealName();
            }
            result.add(WorkerLoadStatItemVO.builder()
                    .workerId(entry.getKey())
                    .workerName(workerName)
                    .count(entry.getValue())
                    .build());
        }

        result.sort(Comparator.comparing(WorkerLoadStatItemVO::getCount).reversed());
        return result;
    }

    private List<RepairTicket> queryTickets(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return repairTicketRepository.findBySubmittedAtBetween(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private List<DispatchRecord> queryDispatchRecords(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return dispatchRecordRepository.findByCreatedAtBetween(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private double calcAvgResponseMinutes(List<RepairTicket> tickets, List<DispatchRecord> records) {
        if (tickets.isEmpty() || records.isEmpty()) {
            return 0.0;
        }
        Map<Long, LocalDateTime> firstDispatchTime = records.stream()
                .collect(Collectors.toMap(
                        DispatchRecord::getTicketId,
                        DispatchRecord::getCreatedAt,
                        (t1, t2) -> t1.isBefore(t2) ? t1 : t2));

        List<Double> values = tickets.stream()
                .filter(t -> firstDispatchTime.containsKey(t.getId()))
                .map(t -> Duration.between(t.getSubmittedAt(), firstDispatchTime.get(t.getId())).toSeconds() / 60.0)
                .filter(v -> v >= 0)
                .toList();

        if (values.isEmpty()) {
            return 0.0;
        }
        return round2(values.stream().mapToDouble(v -> v).average().orElse(0.0));
    }

    private double calcAvgCompleteHours(List<RepairTicket> tickets) {
        List<Double> values = tickets.stream()
                .filter(t -> t.getCompletedAt() != null)
                .map(t -> Duration.between(t.getSubmittedAt(), t.getCompletedAt()).toSeconds() / 3600.0)
                .filter(v -> v >= 0)
                .toList();

        if (values.isEmpty()) {
            return 0.0;
        }
        return round2(values.stream().mapToDouble(v -> v).average().orElse(0.0));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

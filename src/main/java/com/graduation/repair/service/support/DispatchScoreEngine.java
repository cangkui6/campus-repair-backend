package com.graduation.repair.service.support;

import com.graduation.repair.domain.entity.DispatchWeightConfig;
import com.graduation.repair.domain.entity.FaultCategory;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.DispatchScoreVO;
import com.graduation.repair.repository.FaultCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class DispatchScoreEngine {

    private final FaultCategoryRepository faultCategoryRepository;
    private final DispatchWeightManager dispatchWeightManager;

    public DispatchScoreEngine(FaultCategoryRepository faultCategoryRepository,
                               DispatchWeightManager dispatchWeightManager) {
        this.faultCategoryRepository = faultCategoryRepository;
        this.dispatchWeightManager = dispatchWeightManager;
    }

    public List<DispatchScoreVO> rank(RepairTicket ticket, List<MaintenanceWorker> workers) {
        DispatchWeightConfig config = dispatchWeightManager.activeConfig();
        List<DispatchScoreVO> scores = new ArrayList<>();
        for (MaintenanceWorker worker : workers) {
            double skill = skillScore(ticket, worker);
            double area = areaScore(ticket, worker);
            double load = loadScore(worker);
            double perf = perfScore(worker);
            double urgency = urgencyScore(ticket, worker, skill, area);
            double total = round2(100 * (
                    config.getWeightSkill().doubleValue() * skill
                            + config.getWeightArea().doubleValue() * area
                            + config.getWeightLoad().doubleValue() * load
                            + config.getWeightPerf().doubleValue() * perf
                            + config.getWeightUrgency().doubleValue() * urgency));

            scores.add(DispatchScoreVO.builder()
                    .workerId(worker.getId())
                    .scoreSkill(round2(skill * 100))
                    .scoreArea(round2(area * 100))
                    .scoreLoad(round2(load * 100))
                    .scorePerf(round2(perf * 100))
                    .scoreUrgency(round2(urgency * 100))
                    .totalScore(total)
                    .scoreVersion(config.getVersionNo())
                    .build());
        }

        scores.sort(Comparator
                .comparing(DispatchScoreVO::getTotalScore).reversed()
                .thenComparing(DispatchScoreVO::getScoreLoad, Comparator.reverseOrder()));
        return scores;
    }

    private double skillScore(RepairTicket ticket, MaintenanceWorker worker) {
        String categoryCode = resolveCategoryCode(ticket.getCategoryId());
        if (categoryCode.isBlank()) {
            return 0.3;
        }
        String skills = safe(worker.getSkillTags());
        return hasSkillTag(skills, categoryCode) ? 1.0 : 0.3;
    }

    private String resolveCategoryCode(Long categoryId) {
        if (categoryId == null) {
            return "";
        }
        return faultCategoryRepository.findById(categoryId)
                .map(FaultCategory::getCategoryCode)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .orElse("");
    }

    private boolean hasSkillTag(String skillTags, String targetCode) {
        if (skillTags.isBlank() || targetCode.isBlank()) {
            return false;
        }
        String[] tags = skillTags.toUpperCase(Locale.ROOT).split("[,;|\\s]+");
        for (String tag : tags) {
            if (targetCode.equals(tag.trim())) {
                return true;
            }
        }
        return false;
    }

    private double areaScore(RepairTicket ticket, MaintenanceWorker worker) {
        String location = safe(ticket.getLocationText());
        String area = safe(worker.getServiceArea());
        if (location.isBlank() || area.isBlank()) {
            return 0.4;
        }
        String[] chunks = area.split(",");
        for (String chunk : chunks) {
            String c = chunk.trim();
            if (!c.isBlank() && location.contains(c)) {
                return 1.0;
            }
        }
        return 0.4;
    }

    private double loadScore(MaintenanceWorker worker) {
        int load = worker.getCurrentLoad() == null ? 0 : worker.getCurrentLoad();
        return Math.max(0.2, 1 - load * 0.16);
    }

    private double perfScore(MaintenanceWorker worker) {
        double acceptRate = worker.getAcceptRate() == null ? 0.85 : worker.getAcceptRate().doubleValue();
        double avgHours = worker.getAvgCompleteHours() == null ? 24.0 : worker.getAvgCompleteHours().doubleValue();
        int completedCount = worker.getCompletedTicketCount() == null ? 0 : worker.getCompletedTicketCount();
        int reassignCount = worker.getReassignCount() == null ? 0 : worker.getReassignCount();

        double completionScore = avgHours <= 12 ? 1.0 : (avgHours <= 24 ? 0.85 : (avgHours <= 36 ? 0.7 : 0.55));
        double experienceScore = completedCount >= 40 ? 1.0 : (completedCount >= 20 ? 0.85 : (completedCount >= 10 ? 0.75 : 0.65));
        double reassignPenalty = reassignCount >= 8 ? 0.15 : (reassignCount >= 4 ? 0.08 : 0.0);

        return Math.max(0.3, Math.min(1.0, acceptRate * 0.45 + completionScore * 0.35 + experienceScore * 0.20 - reassignPenalty));
    }

    private double urgencyScore(RepairTicket ticket, MaintenanceWorker worker, double skill, double area) {
        String urgency = ticket.getUrgencyLevel() == null ? "LOW" : ticket.getUrgencyLevel().toUpperCase(Locale.ROOT);
        if ("HIGH".equals(urgency)) {
            if (skill >= 1.0 && area >= 1.0) {
                return 1.0;
            }
            if (skill >= 1.0 || area >= 1.0) {
                return 0.7;
            }
            return 0.4;
        }
        if ("MEDIUM".equals(urgency)) {
            return 0.6;
        }
        return 0.4;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

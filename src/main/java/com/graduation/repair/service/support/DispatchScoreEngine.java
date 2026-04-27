package com.graduation.repair.service.support;

import com.graduation.repair.domain.entity.DispatchWeightConfig;
import com.graduation.repair.domain.entity.FaultCategory;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.DispatchScoreVO;
import com.graduation.repair.repository.FaultCategoryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class DispatchScoreEngine {

    private static final int FACTOR_COUNT = 5;

    private final FaultCategoryRepository faultCategoryRepository;
    private final DispatchWeightManager dispatchWeightManager;

    public DispatchScoreEngine(FaultCategoryRepository faultCategoryRepository,
                               DispatchWeightManager dispatchWeightManager) {
        this.faultCategoryRepository = faultCategoryRepository;
        this.dispatchWeightManager = dispatchWeightManager;
    }

    public List<DispatchScoreVO> rank(RepairTicket ticket, List<MaintenanceWorker> workers) {
        DispatchWeightConfig config = dispatchWeightManager.activeConfig();
        List<CandidateScore> candidates = new ArrayList<>();
        for (MaintenanceWorker worker : workers) {
            double skill = skillScore(ticket, worker);
            double area = areaScore(ticket, worker);
            double load = loadScore(worker);
            double perf = perfScore(worker);
            double urgency = urgencyScore(ticket, worker, skill, area);
            candidates.add(new CandidateScore(worker.getId(), new double[]{skill, area, load, perf, urgency}, config.getVersionNo()));
        }

        double[][] weightedMatrix = weightedNormalizedMatrix(candidates, config);
        double[] positiveIdeal = new double[FACTOR_COUNT];
        double[] negativeIdeal = new double[FACTOR_COUNT];
        for (int j = 0; j < FACTOR_COUNT; j++) {
            positiveIdeal[j] = Double.NEGATIVE_INFINITY;
            negativeIdeal[j] = Double.POSITIVE_INFINITY;
            for (int i = 0; i < candidates.size(); i++) {
                positiveIdeal[j] = Math.max(positiveIdeal[j], weightedMatrix[i][j]);
                negativeIdeal[j] = Math.min(negativeIdeal[j], weightedMatrix[i][j]);
            }
        }

        List<DispatchScoreVO> scores = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            CandidateScore candidate = candidates.get(i);
            double positiveDistance = euclideanDistance(weightedMatrix[i], positiveIdeal);
            double negativeDistance = euclideanDistance(weightedMatrix[i], negativeIdeal);
            double closeness = relativeCloseness(positiveDistance, negativeDistance);
            double[] factors = candidate.factors();

            scores.add(DispatchScoreVO.builder()
                    .workerId(candidate.workerId())
                    .scoreSkill(round2(factors[0] * 100))
                    .scoreArea(round2(factors[1] * 100))
                    .scoreLoad(round2(factors[2] * 100))
                    .scorePerf(round2(factors[3] * 100))
                    .scoreUrgency(round2(factors[4] * 100))
                    .totalScore(round2(closeness * 100))
                    .scoreVersion(candidate.scoreVersion())
                    .build());
        }

        scores.sort(Comparator
                .comparing(DispatchScoreVO::getTotalScore).reversed()
                .thenComparing(DispatchScoreVO::getScoreLoad, Comparator.reverseOrder()));
        return scores;
    }

    private double[][] weightedNormalizedMatrix(List<CandidateScore> candidates, DispatchWeightConfig config) {
        double[][] matrix = new double[candidates.size()][FACTOR_COUNT];
        if (candidates.isEmpty()) {
            return matrix;
        }
        double[] weights = normalizedWeights(config);
        double[] denominators = new double[FACTOR_COUNT];
        for (CandidateScore candidate : candidates) {
            double[] factors = candidate.factors();
            for (int j = 0; j < FACTOR_COUNT; j++) {
                denominators[j] += factors[j] * factors[j];
            }
        }
        for (int j = 0; j < FACTOR_COUNT; j++) {
            denominators[j] = Math.sqrt(denominators[j]);
        }
        for (int i = 0; i < candidates.size(); i++) {
            double[] factors = candidates.get(i).factors();
            for (int j = 0; j < FACTOR_COUNT; j++) {
                double normalized = denominators[j] == 0 ? 0 : factors[j] / denominators[j];
                matrix[i][j] = normalized * weights[j];
            }
        }
        return matrix;
    }

    private double[] normalizedWeights(DispatchWeightConfig config) {
        double[] weights = new double[]{
                positive(config.getWeightSkill()),
                positive(config.getWeightArea()),
                positive(config.getWeightLoad()),
                positive(config.getWeightPerf()),
                positive(config.getWeightUrgency())
        };
        double total = 0;
        for (double weight : weights) {
            total += weight;
        }
        if (total <= 0) {
            return new double[]{0.35, 0.20, 0.20, 0.10, 0.15};
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weights[i] / total;
        }
        return weights;
    }

    private double positive(BigDecimal value) {
        return value == null ? 0.0 : Math.max(0.0, value.doubleValue());
    }

    private double euclideanDistance(double[] current, double[] target) {
        double sum = 0;
        for (int i = 0; i < current.length; i++) {
            double diff = current[i] - target[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private double relativeCloseness(double positiveDistance, double negativeDistance) {
        double denominator = positiveDistance + negativeDistance;
        if (denominator == 0) {
            return 1.0;
        }
        return negativeDistance / denominator;
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

    private record CandidateScore(Long workerId, double[] factors, Integer scoreVersion) {
    }
}

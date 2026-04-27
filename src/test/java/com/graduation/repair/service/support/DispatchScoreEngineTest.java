package com.graduation.repair.service.support;

import com.graduation.repair.domain.entity.DispatchWeightConfig;
import com.graduation.repair.domain.entity.FaultCategory;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.DispatchScoreVO;
import com.graduation.repair.repository.FaultCategoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

class DispatchScoreEngineTest {

    private FaultCategoryRepository faultCategoryRepository;
    private DispatchScoreEngine dispatchScoreEngine;

    @BeforeEach
    void setUp() {
        faultCategoryRepository = Mockito.mock(FaultCategoryRepository.class);
        DispatchWeightManager dispatchWeightManager = Mockito.mock(DispatchWeightManager.class);
        DispatchWeightConfig config = new DispatchWeightConfig();
        config.setVersionNo(1);
        config.setWeightSkill(BigDecimal.valueOf(0.35));
        config.setWeightArea(BigDecimal.valueOf(0.20));
        config.setWeightLoad(BigDecimal.valueOf(0.20));
        config.setWeightPerf(BigDecimal.valueOf(0.10));
        config.setWeightUrgency(BigDecimal.valueOf(0.15));
        Mockito.when(dispatchWeightManager.activeConfig()).thenReturn(config);
        dispatchScoreEngine = new DispatchScoreEngine(faultCategoryRepository, dispatchWeightManager);
    }

    @Test
    void skillMatch_shouldUseCategoryCodeInsteadOfCategoryId() {
        mockCategory("AIR_CONDITIONER");

        RepairTicket ticket = ticket("MEDIUM", "教学楼片区");

        MaintenanceWorker workerA = worker(1L, "AIR_CONDITIONER,LIGHTING", "教学楼片区", 1);
        MaintenanceWorker workerB = worker(2L, "NETWORK,WATER_ELECTRIC", "教学楼片区", 1);

        List<DispatchScoreVO> ranking = dispatchScoreEngine.rank(ticket, List.of(workerA, workerB));

        Assertions.assertEquals(1L, ranking.get(0).getWorkerId());
        Assertions.assertTrue(ranking.get(0).getScoreSkill() > ranking.get(1).getScoreSkill());
    }

    @Test
    void rank_shouldUseTopsisClosenessForCandidateOrdering() {
        mockCategory("AIR_CONDITIONER");

        RepairTicket ticket = ticket("HIGH", "教学楼片区A座");

        MaintenanceWorker best = worker(1L, "AIR_CONDITIONER,LIGHTING", "教学楼片区", 1);
        best.setAcceptRate(BigDecimal.valueOf(0.95));
        best.setAvgCompleteHours(BigDecimal.valueOf(10));
        best.setCompletedTicketCount(45);
        best.setReassignCount(0);

        MaintenanceWorker busy = worker(2L, "AIR_CONDITIONER", "教学楼片区", 4);
        busy.setAcceptRate(BigDecimal.valueOf(0.80));
        busy.setAvgCompleteHours(BigDecimal.valueOf(30));
        busy.setCompletedTicketCount(12);
        busy.setReassignCount(5);

        MaintenanceWorker mismatch = worker(3L, "NETWORK", "宿舍区", 0);
        mismatch.setAcceptRate(BigDecimal.valueOf(0.90));
        mismatch.setAvgCompleteHours(BigDecimal.valueOf(12));
        mismatch.setCompletedTicketCount(30);
        mismatch.setReassignCount(0);

        List<DispatchScoreVO> ranking = dispatchScoreEngine.rank(ticket, List.of(busy, mismatch, best));

        Assertions.assertEquals(1L, ranking.get(0).getWorkerId());
        Assertions.assertEquals(3, ranking.size());
        Assertions.assertTrue(ranking.get(0).getTotalScore() > ranking.get(1).getTotalScore());
        Assertions.assertTrue(ranking.get(2).getTotalScore() >= 0);
        Assertions.assertTrue(ranking.get(0).getTotalScore() <= 100);
    }

    @Test
    void rank_whenAllCandidatesSame_shouldKeepValidTopsisScoreAndUseLoadTieBreaker() {
        mockCategory("NETWORK");

        RepairTicket ticket = ticket("LOW", "宿舍区");
        MaintenanceWorker workerA = worker(1L, "NETWORK", "宿舍区", 2);
        MaintenanceWorker workerB = worker(2L, "NETWORK", "宿舍区", 2);

        List<DispatchScoreVO> ranking = dispatchScoreEngine.rank(ticket, List.of(workerA, workerB));

        Assertions.assertEquals(100.0, ranking.get(0).getTotalScore());
        Assertions.assertEquals(100.0, ranking.get(1).getTotalScore());
    }

    @Test
    void rank_whenWorkerListEmpty_shouldReturnEmptyRanking() {
        mockCategory("NETWORK");

        List<DispatchScoreVO> ranking = dispatchScoreEngine.rank(ticket("LOW", "宿舍区"), List.of());

        Assertions.assertTrue(ranking.isEmpty());
    }

    private void mockCategory(String categoryCode) {
        FaultCategory category = new FaultCategory();
        category.setId(4L);
        category.setCategoryCode(categoryCode);
        Mockito.when(faultCategoryRepository.findById(4L)).thenReturn(Optional.of(category));
    }

    private RepairTicket ticket(String urgencyLevel, String locationText) {
        RepairTicket ticket = new RepairTicket();
        ticket.setCategoryId(4L);
        ticket.setUrgencyLevel(urgencyLevel);
        ticket.setLocationText(locationText);
        return ticket;
    }

    private MaintenanceWorker worker(Long id, String skills, String serviceArea, int currentLoad) {
        MaintenanceWorker worker = new MaintenanceWorker();
        worker.setId(id);
        worker.setSkillTags(skills);
        worker.setServiceArea(serviceArea);
        worker.setCurrentLoad(currentLoad);
        return worker;
    }
}

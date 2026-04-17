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
        FaultCategory category = new FaultCategory();
        category.setId(4L);
        category.setCategoryCode("AIR_CONDITIONER");
        Mockito.when(faultCategoryRepository.findById(4L)).thenReturn(Optional.of(category));

        RepairTicket ticket = new RepairTicket();
        ticket.setCategoryId(4L);
        ticket.setUrgencyLevel("MEDIUM");
        ticket.setLocationText("教学楼片区");

        MaintenanceWorker workerA = new MaintenanceWorker();
        workerA.setId(1L);
        workerA.setSkillTags("AIR_CONDITIONER,LIGHTING");
        workerA.setServiceArea("教学楼片区");
        workerA.setCurrentLoad(1);

        MaintenanceWorker workerB = new MaintenanceWorker();
        workerB.setId(2L);
        workerB.setSkillTags("NETWORK,WATER_ELECTRIC");
        workerB.setServiceArea("教学楼片区");
        workerB.setCurrentLoad(1);

        List<DispatchScoreVO> ranking = dispatchScoreEngine.rank(ticket, List.of(workerA, workerB));

        Assertions.assertEquals(1L, ranking.get(0).getWorkerId());
        Assertions.assertTrue(ranking.get(0).getScoreSkill() > ranking.get(1).getScoreSkill());
    }
}

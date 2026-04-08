package com.graduation.repair.service.support;

import com.graduation.repair.domain.entity.DispatchWeightConfig;
import com.graduation.repair.repository.DispatchWeightConfigRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DispatchWeightManager {

    private final DispatchWeightConfigRepository dispatchWeightConfigRepository;

    public DispatchWeightManager(DispatchWeightConfigRepository dispatchWeightConfigRepository) {
        this.dispatchWeightConfigRepository = dispatchWeightConfigRepository;
    }

    public DispatchWeightConfig activeConfig() {
        return dispatchWeightConfigRepository.findFirstByIsActiveOrderByVersionNoDesc(1)
                .orElseGet(this::defaultConfig);
    }

    @Transactional
    public DispatchWeightConfig activateNext(double skill, double area, double load, double perf, double urgency, String triggerSource) {
        dispatchWeightConfigRepository.findFirstByIsActiveOrderByVersionNoDesc(1)
                .ifPresent(current -> {
                    current.setIsActive(0);
                    dispatchWeightConfigRepository.save(current);
                });
        int version = dispatchWeightConfigRepository.findTopByOrderByVersionNoDesc()
                .map(item -> item.getVersionNo() + 1)
                .orElse(1);
        DispatchWeightConfig config = new DispatchWeightConfig();
        config.setVersionNo(version);
        config.setWeightSkill(decimal(skill));
        config.setWeightArea(decimal(area));
        config.setWeightLoad(decimal(load));
        config.setWeightPerf(decimal(perf));
        config.setWeightUrgency(decimal(urgency));
        config.setTriggerSource(triggerSource);
        config.setIsActive(1);
        config.setCreatedAt(LocalDateTime.now());
        return dispatchWeightConfigRepository.save(config);
    }

    private DispatchWeightConfig defaultConfig() {
        return activateNext(0.35, 0.20, 0.20, 0.10, 0.15, "SYSTEM_INIT");
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}

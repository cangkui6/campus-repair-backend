package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.DispatchWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispatchWeightConfigRepository extends JpaRepository<DispatchWeightConfig, Long> {

    Optional<DispatchWeightConfig> findFirstByIsActiveOrderByVersionNoDesc(Integer isActive);

    Optional<DispatchWeightConfig> findTopByOrderByVersionNoDesc();
}

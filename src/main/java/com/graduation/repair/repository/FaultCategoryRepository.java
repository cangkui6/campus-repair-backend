package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.FaultCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FaultCategoryRepository extends JpaRepository<FaultCategory, Long> {

    Optional<FaultCategory> findByCategoryCode(String categoryCode);
}

package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.MaintenanceWorker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceWorkerRepository extends JpaRepository<MaintenanceWorker, Long> {

    List<MaintenanceWorker> findByIsAvailable(Integer isAvailable);

    Optional<MaintenanceWorker> findByUserId(Long userId);
}

package com.graduation.repair.service.impl;

import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.SysUser;
import com.graduation.repair.domain.vo.WorkerOptionVO;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.SysUserRepository;
import com.graduation.repair.service.WorkerAdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkerAdminServiceImpl implements WorkerAdminService {

    private final MaintenanceWorkerRepository maintenanceWorkerRepository;
    private final SysUserRepository sysUserRepository;

    public WorkerAdminServiceImpl(MaintenanceWorkerRepository maintenanceWorkerRepository,
                                  SysUserRepository sysUserRepository) {
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
        this.sysUserRepository = sysUserRepository;
    }

    @Override
    public List<WorkerOptionVO> listWorkers() {
        Map<Long, SysUser> users = sysUserRepository.findByRole("WORKER").stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        return maintenanceWorkerRepository.findAll().stream()
                .map(worker -> {
                    SysUser user = users.get(worker.getUserId());
                    return WorkerOptionVO.builder()
                            .workerId(worker.getId())
                            .userId(worker.getUserId())
                            .workerName(user == null ? ("维修人员-" + worker.getId()) : user.getRealName())
                            .skillTags(worker.getSkillTags())
                            .serviceArea(worker.getServiceArea())
                            .currentLoad(worker.getCurrentLoad())
                            .isAvailable(worker.getIsAvailable())
                            .build();
                })
                .toList();
    }
}

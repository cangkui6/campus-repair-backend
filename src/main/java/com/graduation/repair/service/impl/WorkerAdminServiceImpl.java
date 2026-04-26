package com.graduation.repair.service.impl;

import com.graduation.repair.common.enums.UserRole;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.AdminResetPasswordRequest;
import com.graduation.repair.domain.dto.WorkerCreateRequest;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.SysUser;
import com.graduation.repair.domain.vo.AccountCreateVO;
import com.graduation.repair.domain.vo.WorkerOptionVO;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.SysUserRepository;
import com.graduation.repair.service.WorkerAdminService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkerAdminServiceImpl implements WorkerAdminService {

    private final MaintenanceWorkerRepository maintenanceWorkerRepository;
    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    public WorkerAdminServiceImpl(MaintenanceWorkerRepository maintenanceWorkerRepository,
                                  SysUserRepository sysUserRepository,
                                  PasswordEncoder passwordEncoder) {
        this.maintenanceWorkerRepository = maintenanceWorkerRepository;
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
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
                            .avgCompleteHours(worker.getAvgCompleteHours())
                            .acceptRate(worker.getAcceptRate())
                            .completedTicketCount(worker.getCompletedTicketCount())
                            .reassignCount(worker.getReassignCount())
                            .lastActiveAt(worker.getLastActiveAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public AccountCreateVO createWorker(WorkerCreateRequest request) {
        String username = request.getUsername().trim();
        if (sysUserRepository.existsByUsername(username)) {
            throw new BizException(4091, "用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole(UserRole.WORKER.name());
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        SysUser savedUser = sysUserRepository.save(user);

        MaintenanceWorker worker = new MaintenanceWorker();
        worker.setUserId(savedUser.getId());
        worker.setSkillTags(request.getSkillTags().trim());
        worker.setServiceArea(request.getServiceArea().trim());
        worker.setCurrentLoad(0);
        worker.setIsAvailable(1);
        worker.setAvgCompleteHours(BigDecimal.valueOf(24.00));
        worker.setAcceptRate(BigDecimal.valueOf(0.8500));
        worker.setCompletedTicketCount(0);
        worker.setReassignCount(0);
        worker.setCreatedAt(now);
        worker.setUpdatedAt(now);
        MaintenanceWorker savedWorker = maintenanceWorkerRepository.save(worker);

        return AccountCreateVO.builder()
                .userId(savedUser.getId())
                .workerId(savedWorker.getId())
                .username(savedUser.getUsername())
                .role(savedUser.getRole())
                .realName(savedUser.getRealName())
                .build();
    }

    @Override
    @Transactional
    public void resetUserPassword(Long operatorId, AdminResetPasswordRequest request) {
        if (operatorId.equals(request.getUserId())) {
            throw new BizException(4003, "管理员不能在此处重置自己的密码，请使用修改密码功能");
        }
        SysUser user = sysUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new BizException(4040, "用户不存在"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserRepository.save(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

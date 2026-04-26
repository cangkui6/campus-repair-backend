package com.graduation.repair.service;

import com.graduation.repair.domain.dto.AdminResetPasswordRequest;
import com.graduation.repair.domain.dto.WorkerCreateRequest;
import com.graduation.repair.domain.vo.AccountCreateVO;
import com.graduation.repair.domain.vo.WorkerOptionVO;

import java.util.List;

public interface WorkerAdminService {

    List<WorkerOptionVO> listWorkers();

    AccountCreateVO createWorker(WorkerCreateRequest request);

    void resetUserPassword(Long operatorId, AdminResetPasswordRequest request);
}

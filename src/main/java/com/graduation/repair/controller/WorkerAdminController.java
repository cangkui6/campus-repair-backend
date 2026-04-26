package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.dto.AdminResetPasswordRequest;
import com.graduation.repair.domain.dto.WorkerCreateRequest;
import com.graduation.repair.domain.vo.AccountCreateVO;
import com.graduation.repair.domain.vo.WorkerOptionVO;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.WorkerAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/workers")
public class WorkerAdminController {

    private final WorkerAdminService workerAdminService;

    public WorkerAdminController(WorkerAdminService workerAdminService) {
        this.workerAdminService = workerAdminService;
    }

    @GetMapping
    public ApiResponse<List<WorkerOptionVO>> listWorkers() {
        return ApiResponse.success(workerAdminService.listWorkers());
    }

    @PostMapping
    public ApiResponse<AccountCreateVO> createWorker(@Valid @RequestBody WorkerCreateRequest request) {
        return ApiResponse.success(workerAdminService.createWorker(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AdminResetPasswordRequest request) {
        AuthUser authUser = SecurityUserContext.currentUser();
        workerAdminService.resetUserPassword(authUser.getUserId(), request);
        return ApiResponse.success(null);
    }
}

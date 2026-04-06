package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.vo.WorkerOptionVO;
import com.graduation.repair.service.WorkerAdminService;
import org.springframework.web.bind.annotation.GetMapping;
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
}

package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.vo.WorkerTaskListItemVO;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.WorkerTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker")
public class WorkerTaskController {

    private final WorkerTaskService workerTaskService;

    public WorkerTaskController(WorkerTaskService workerTaskService) {
        this.workerTaskService = workerTaskService;
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResult<WorkerTaskListItemVO>> myTasks(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String status
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(workerTaskService.myTasks(user.getUserId(), user.getRole(), page, size, status));
    }
}

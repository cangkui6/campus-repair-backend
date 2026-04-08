package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.WorkerRejectRequest;
import com.graduation.repair.domain.vo.WorkerHistoryItemVO;
import com.graduation.repair.domain.vo.WorkerProfileVO;
import com.graduation.repair.domain.vo.WorkerTaskListItemVO;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.WorkerTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/available-tasks")
    public ApiResponse<PageResult<WorkerTaskListItemVO>> availableTasks(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(workerTaskService.availableTasks(user.getUserId(), user.getRole(), page, size));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<WorkerHistoryItemVO>> myHistory(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(workerTaskService.myHistory(user.getUserId(), user.getRole(), page, size));
    }

    @GetMapping("/profile")
    public ApiResponse<WorkerProfileVO> myProfile() {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(workerTaskService.myProfile(user.getUserId(), user.getRole()));
    }

    @PostMapping("/tasks/{ticketId}/grab")
    public ApiResponse<Void> grabTask(@PathVariable Long ticketId) {
        AuthUser user = SecurityUserContext.currentUser();
        workerTaskService.grabTask(user.getUserId(), user.getRole(), ticketId);
        return ApiResponse.success(null);
    }

    @PostMapping("/tasks/{ticketId}/reject")
    public ApiResponse<Void> rejectTask(@PathVariable Long ticketId, @RequestBody(required = false) WorkerRejectRequest request) {
        AuthUser user = SecurityUserContext.currentUser();
        workerTaskService.rejectTask(user.getUserId(), user.getRole(), ticketId, request == null ? null : request.getReason());
        return ApiResponse.success(null);
    }
}

package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.ManualParseConfirmRequest;
import com.graduation.repair.domain.vo.LlmAuditLogItemVO;
import com.graduation.repair.domain.vo.ReviewQueueItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.LlmAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/llm")
public class LlmAdminController {

    private final LlmAdminService llmAdminService;

    public LlmAdminController(LlmAdminService llmAdminService) {
        this.llmAdminService = llmAdminService;
    }

    @GetMapping("/review-queue")
    public ApiResponse<PageResult<ReviewQueueItemVO>> reviewQueue(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String queueStatus
    ) {
        return ApiResponse.success(llmAdminService.reviewQueue(page, size, queueStatus));
    }

    @PostMapping("/review-queue/{queueId}/confirm")
    public ApiResponse<TicketStatusChangeResponse> confirm(
            @PathVariable Long queueId,
            @RequestBody ManualParseConfirmRequest request
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(llmAdminService.confirmReview(user.getUserId(), queueId, request));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<LlmAuditLogItemVO>> auditLogs(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String parseStatus,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String promptVersion,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        return ApiResponse.success(llmAdminService.auditLogs(page, size, ticketNo, parseStatus, modelName, promptVersion,
                parseDateTime(startTime), parseDateTime(endTime)));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}

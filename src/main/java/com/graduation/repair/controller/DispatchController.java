package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.dto.DispatchManualRequest;
import com.graduation.repair.domain.dto.DispatchRetryRequest;
import com.graduation.repair.domain.vo.DispatchResultVO;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.DispatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/auto")
    public ApiResponse<DispatchResultVO> autoDispatch(@PathVariable Long ticketId) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(dispatchService.autoDispatch(user.getUserId(), user.getRole(), ticketId));
    }

    @PostMapping("/manual")
    public ApiResponse<DispatchResultVO> manualDispatch(
            @PathVariable Long ticketId,
            @Valid @RequestBody DispatchManualRequest request
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(dispatchService.manualDispatch(user.getUserId(), user.getRole(), ticketId, request));
    }

    @PostMapping("/retry")
    public ApiResponse<DispatchResultVO> retryDispatch(
            @PathVariable Long ticketId,
            @Valid @RequestBody DispatchRetryRequest request
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(dispatchService.retryDispatch(user.getUserId(), user.getRole(), ticketId, request));
    }
}

package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.dto.DispatchWeightUpdateRequest;
import com.graduation.repair.domain.vo.DispatchFeedbackOverviewVO;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.DispatchFeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dispatch-feedback")
public class DispatchFeedbackAdminController {

    private final DispatchFeedbackService dispatchFeedbackService;

    public DispatchFeedbackAdminController(DispatchFeedbackService dispatchFeedbackService) {
        this.dispatchFeedbackService = dispatchFeedbackService;
    }

    @GetMapping
    public ApiResponse<DispatchFeedbackOverviewVO> overview() {
        return ApiResponse.success(dispatchFeedbackService.overview());
    }

    @PostMapping("/recalculate")
    public ApiResponse<DispatchFeedbackOverviewVO> recalculate() {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(dispatchFeedbackService.recalculate(user.getUserId()));
    }

    @PostMapping("/weights")
    public ApiResponse<DispatchFeedbackOverviewVO> updateWeights(@RequestBody DispatchWeightUpdateRequest request) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(dispatchFeedbackService.updateWeights(user.getUserId(), request));
    }

    @PostMapping("/weights/default")
    public ApiResponse<DispatchFeedbackOverviewVO> resetDefaultWeights() {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(dispatchFeedbackService.resetDefaultWeights(user.getUserId()));
    }
}

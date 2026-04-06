package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.dto.LlmClassifyRequest;
import com.graduation.repair.domain.dto.LlmParseRequest;
import com.graduation.repair.domain.vo.LlmClassifyResponse;
import com.graduation.repair.domain.vo.LlmHealthVO;
import com.graduation.repair.domain.vo.LlmParseResponse;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.LlmService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/llm")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/parse")
    public ApiResponse<LlmParseResponse> parse(@RequestBody LlmParseRequest request) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(llmService.parse(request, user.getUserId()));
    }

    @PostMapping("/classify")
    public ApiResponse<LlmClassifyResponse> classify(@Valid @RequestBody LlmClassifyRequest request) {
        return ApiResponse.success(llmService.classify(request));
    }

    @GetMapping("/health")
    public ApiResponse<LlmHealthVO> health() {
        return ApiResponse.success(llmService.health());
    }
}

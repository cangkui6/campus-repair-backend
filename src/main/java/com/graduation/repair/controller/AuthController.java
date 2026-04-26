package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.dto.ChangePasswordRequest;
import com.graduation.repair.domain.dto.ForgotPasswordRequest;
import com.graduation.repair.domain.dto.LoginRequest;
import com.graduation.repair.domain.dto.RegisterRequest;
import com.graduation.repair.domain.vo.CurrentUserVO;
import com.graduation.repair.domain.vo.LoginResponse;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resetPasswordByPhone(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthUser authUser = SecurityUserContext.currentUser();
        authService.changePassword(authUser.getUserId(), request);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me() {
        AuthUser authUser = SecurityUserContext.currentUser();
        return ApiResponse.success(authService.currentUser(authUser.getUserId()));
    }
}

package com.graduation.repair.service;

import com.graduation.repair.domain.dto.ChangePasswordRequest;
import com.graduation.repair.domain.dto.ForgotPasswordRequest;
import com.graduation.repair.domain.dto.LoginRequest;
import com.graduation.repair.domain.dto.RegisterRequest;
import com.graduation.repair.domain.vo.CurrentUserVO;
import com.graduation.repair.domain.vo.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void resetPasswordByPhone(ForgotPasswordRequest request);

    CurrentUserVO currentUser(Long userId);
}

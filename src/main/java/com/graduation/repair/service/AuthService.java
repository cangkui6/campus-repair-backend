package com.graduation.repair.service;

import com.graduation.repair.domain.dto.LoginRequest;
import com.graduation.repair.domain.vo.CurrentUserVO;
import com.graduation.repair.domain.vo.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    CurrentUserVO currentUser(Long userId);
}

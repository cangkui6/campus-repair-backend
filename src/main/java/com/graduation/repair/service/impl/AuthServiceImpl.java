package com.graduation.repair.service.impl;

import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.LoginRequest;
import com.graduation.repair.domain.entity.SysUser;
import com.graduation.repair.domain.vo.CurrentUserVO;
import com.graduation.repair.domain.vo.LoginResponse;
import com.graduation.repair.repository.SysUserRepository;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.JwtTokenProvider;
import com.graduation.repair.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserRepository sysUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(SysUserRepository sysUserRepository,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.sysUserRepository = sysUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BizException(4011, "用户名或密码错误"));

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(4012, "账号已禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(4011, "用户名或密码错误");
        }

        String token = jwtTokenProvider.createToken(new AuthUser(user.getId(), user.getUsername(), user.getRole()));
        return new LoginResponse(token, user.getRole());
    }

    @Override
    public CurrentUserVO currentUser(Long userId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(4040, "用户不存在"));
        return new CurrentUserVO(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }
}

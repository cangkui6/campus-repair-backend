package com.graduation.repair.service.impl;

import com.graduation.repair.common.enums.UserRole;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.ChangePasswordRequest;
import com.graduation.repair.domain.dto.ForgotPasswordRequest;
import com.graduation.repair.domain.dto.LoginRequest;
import com.graduation.repair.domain.dto.RegisterRequest;
import com.graduation.repair.domain.entity.SysUser;
import com.graduation.repair.domain.vo.CurrentUserVO;
import com.graduation.repair.domain.vo.LoginResponse;
import com.graduation.repair.repository.SysUserRepository;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.JwtTokenProvider;
import com.graduation.repair.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        return buildLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (sysUserRepository.existsByUsername(username)) {
            throw new BizException(4091, "用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole(UserRole.REPORTER.name());
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        SysUser saved = sysUserRepository.save(user);

        return buildLoginResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(4040, "用户不存在"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(4013, "原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BizException(4002, "新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPasswordByPhone(ForgotPasswordRequest request) {
        SysUser user = sysUserRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BizException(4040, "用户不存在"));
        if (user.getPhone() == null || !user.getPhone().equals(request.getPhone().trim())) {
            throw new BizException(4014, "用户名与预留手机号不匹配");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BizException(4002, "新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserRepository.save(user);
    }

    @Override
    public CurrentUserVO currentUser(Long userId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(4040, "用户不存在"));
        return new CurrentUserVO(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        String token = jwtTokenProvider.createToken(new AuthUser(user.getId(), user.getUsername(), user.getRole()));
        return new LoginResponse(token, user.getRole());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

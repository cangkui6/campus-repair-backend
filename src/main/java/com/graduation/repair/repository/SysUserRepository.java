package com.graduation.repair.repository;

import com.graduation.repair.domain.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    List<SysUser> findByRole(String role);
}

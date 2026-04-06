package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserVO {

    private Long userId;
    private String username;
    private String realName;
    private String role;
}

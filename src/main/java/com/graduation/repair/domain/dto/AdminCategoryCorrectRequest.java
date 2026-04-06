package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCategoryCorrectRequest {

    @NotNull(message = "categoryId不能为空")
    private Long categoryId;

    @NotBlank(message = "reason不能为空")
    private String reason;
}

package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminRollbackRequest {

    @NotBlank(message = "targetStatus不能为空")
    private String targetStatus;

    @NotBlank(message = "reason不能为空")
    private String reason;
}

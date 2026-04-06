package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DispatchManualRequest {

    @NotNull(message = "workerId不能为空")
    private Long workerId;

    private String reason;
}

package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DispatchRetryRequest {

    @NotNull(message = "rejectingWorkerId不能为空")
    private Long rejectingWorkerId;
}

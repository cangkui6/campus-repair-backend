package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketCompleteRequest {

    @NotBlank(message = "处理结果不能为空")
    private String repairResult;
}

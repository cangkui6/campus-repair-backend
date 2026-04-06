package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketCreateRequest {

    @NotBlank(message = "报修内容不能为空")
    private String rawText;

    private String contactPhone;
}

package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LlmClassifyRequest {

    @NotBlank(message = "rawText不能为空")
    private String rawText;

    private String location;
}

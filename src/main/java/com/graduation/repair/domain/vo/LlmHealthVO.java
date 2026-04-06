package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LlmHealthVO {

    private String provider;
    private String status;
    private Long latencyMs;
}

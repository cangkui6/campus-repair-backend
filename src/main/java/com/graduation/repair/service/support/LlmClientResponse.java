package com.graduation.repair.service.support;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LlmClientResponse {

    private String rawResponse;
    private long latencyMs;
}

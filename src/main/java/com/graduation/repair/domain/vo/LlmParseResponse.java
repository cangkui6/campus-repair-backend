package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LlmParseResponse {

    private String category;
    private String location;
    private String faultPhenomenon;
    private String urgency;
    private String contact;
    private String timePreference;
    private Double confidence;
    private String parseStatus;
    private String promptVersion;
    private String modelName;
    private Long latencyMs;
    private Boolean fallbackQueued;
}

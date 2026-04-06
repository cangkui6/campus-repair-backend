package com.graduation.repair.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class LlmParseRequest {

    private Long ticketId;
    private String rawText;
    private Map<String, Object> extraContext;
}

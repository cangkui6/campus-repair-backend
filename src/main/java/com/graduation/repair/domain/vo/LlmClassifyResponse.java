package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LlmClassifyResponse {

    private String category;
    private Double confidence;
}

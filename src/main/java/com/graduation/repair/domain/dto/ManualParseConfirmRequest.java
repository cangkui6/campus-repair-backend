package com.graduation.repair.domain.dto;

import lombok.Data;

@Data
public class ManualParseConfirmRequest {

    private String locationText;
    private String faultDesc;
    private String urgencyLevel;
    private Long categoryId;
}

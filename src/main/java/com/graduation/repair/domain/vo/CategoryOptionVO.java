package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryOptionVO {

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
}

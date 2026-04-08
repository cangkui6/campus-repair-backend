package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.vo.CategoryOptionVO;
import com.graduation.repair.repository.FaultCategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final FaultCategoryRepository faultCategoryRepository;

    public AdminCategoryController(FaultCategoryRepository faultCategoryRepository) {
        this.faultCategoryRepository = faultCategoryRepository;
    }

    @GetMapping
    public ApiResponse<List<CategoryOptionVO>> list() {
        return ApiResponse.success(faultCategoryRepository.findAll().stream()
                .map(item -> CategoryOptionVO.builder()
                        .categoryId(item.getId())
                        .categoryCode(item.getCategoryCode())
                        .categoryName(item.getCategoryName())
                        .build())
                .toList());
    }
}

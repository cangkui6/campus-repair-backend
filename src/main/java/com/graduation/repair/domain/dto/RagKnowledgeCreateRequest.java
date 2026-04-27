package com.graduation.repair.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RagKnowledgeCreateRequest {

    @NotBlank(message = "知识类型不能为空")
    @Size(max = 64)
    private String type = "policy";

    @NotBlank(message = "标题不能为空")
    @Size(max = 120)
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(min = 5, max = 2000)
    private String content;

    @Size(max = 12)
    private List<String> tags = List.of();

    @Size(max = 120)
    private String source = "管理员维护";

    @Min(1)
    @Max(10)
    private Integer priority = 1;
}

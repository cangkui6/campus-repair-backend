package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.domain.dto.RagKnowledgeCreateRequest;
import com.graduation.repair.integration.rag.RagClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/rag-kb")
public class RagKnowledgeAdminController {

    private final RagClient ragClient;

    public RagKnowledgeAdminController(RagClient ragClient) {
        this.ragClient = ragClient;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listKnowledge() {
        return ApiResponse.success(ragClient.listKnowledge());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> addKnowledge(@Valid @RequestBody RagKnowledgeCreateRequest request) {
        Map<String, Object> payload = Map.of(
                "type", request.getType(),
                "title", request.getTitle(),
                "content", request.getContent(),
                "tags", request.getTags(),
                "source", request.getSource(),
                "priority", request.getPriority()
        );
        return ApiResponse.success(ragClient.addKnowledge(payload));
    }

    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuildKnowledge() {
        return ApiResponse.success(ragClient.rebuildKnowledge());
    }
}

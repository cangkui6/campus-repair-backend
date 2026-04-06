package com.graduation.repair.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.repair.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class ZhipuLlmClient implements LlmClientAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String chatPath;
    private final String model;
    private final String apiKey;

    public ZhipuLlmClient(RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper,
                          @Value("${llm.base-url}") String baseUrl,
                          @Value("${llm.chat-path}") String chatPath,
                          @Value("${llm.model}") String model,
                          @Value("${llm.api-key}") String apiKey,
                          @Value("${llm.timeout-seconds:30}") Long timeoutSeconds) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.chatPath = chatPath;
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public LlmClientResponse chatJson(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + chatPath,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new BizException(4104, "智谱返回结果为空");
            }
            return new LlmClientResponse(stripCodeFence(contentNode.asText()), System.currentTimeMillis() - start);
        } catch (RestClientException e) {
            throw new BizException(4105, "智谱接口调用失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BizException(4106, "智谱返回解析失败: " + e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "ZHIPU_GLM";
    }

    @Override
    public String modelName() {
        return model;
    }

    private String stripCodeFence(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }
        return text;
    }
}

package com.graduation.repair.integration.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RagClient {

    private static final Logger log = LoggerFactory.getLogger(RagClient.class);

    private final RestTemplate restTemplate;
    private final RagProperties properties;

    public RagClient(RestTemplateBuilder restTemplateBuilder, RagProperties properties) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    public List<RagRetrieveItem> retrieve(String query, int topK) {
        if (!isEnabled() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        int actualTopK = topK > 0 ? topK : properties.getTopK();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "query", query,
                "topK", actualTopK
        );
        ResponseEntity<RagRetrieveResponse> response = restTemplate.postForEntity(
                properties.getBaseUrl() + properties.getRetrievePath(),
                new HttpEntity<>(body, headers),
                RagRetrieveResponse.class
        );
        RagRetrieveResponse ragResponse = response.getBody();
        if (ragResponse == null || ragResponse.getCode() == null || ragResponse.getCode() != 0 || ragResponse.getData() == null) {
            log.warn("RAG retrieve returned empty or invalid response. query={} topK={}", query, actualTopK);
            return Collections.emptyList();
        }
        List<RagRetrieveItem> items = ragResponse.getData().getItems();
        List<RagRetrieveItem> result = items == null ? Collections.emptyList() : items;
        log.info("RAG retrieve success. query={} topK={} hitCount={} hitIds={}", query, actualTopK, result.size(), result.stream().map(RagRetrieveItem::getId).toList());
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> listKnowledge() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                properties.getBaseUrl() + properties.getKnowledgePath(),
                Map.class
        );
        return response.getBody() == null ? Collections.emptyMap() : response.getBody();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> addKnowledge(Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                properties.getBaseUrl() + properties.getKnowledgePath(),
                new HttpEntity<>(request, headers),
                Map.class
        );
        return response.getBody() == null ? Collections.emptyMap() : response.getBody();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> rebuildKnowledge() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                properties.getBaseUrl() + properties.getKnowledgeRebuildPath(),
                HttpEntity.EMPTY,
                Map.class
        );
        return response.getBody() == null ? Collections.emptyMap() : response.getBody();
    }

    public boolean available() {
        if (!isEnabled()) {
            return false;
        }
        try {
            ResponseEntity<RagHealthResponse> response = restTemplate.getForEntity(
                    properties.getBaseUrl() + properties.getHealthPath(),
                    RagHealthResponse.class
            );
            RagHealthResponse body = response.getBody();
            return body != null
                    && body.getCode() != null
                    && body.getCode() == 0
                    && body.getData() != null
                    && "UP".equalsIgnoreCase(body.getData().getStatus());
        } catch (RestClientException ex) {
            return false;
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public int topK() {
        return properties.getTopK();
    }
}


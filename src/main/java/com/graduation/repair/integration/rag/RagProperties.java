package com.graduation.repair.integration.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RagProperties {

    private final boolean enabled;
    private final String baseUrl;
    private final String retrievePath;
    private final String healthPath;
    private final String knowledgePath;
    private final String knowledgeRebuildPath;
    private final int topK;
    private final long timeoutSeconds;

    public RagProperties(@Value("${rag.enabled:false}") boolean enabled,
                         @Value("${rag.base-url:http://localhost:9001}") String baseUrl,
                         @Value("${rag.retrieve-path:/api/v1/rag/retrieve}") String retrievePath,
                         @Value("${rag.health-path:/api/v1/rag/health}") String healthPath,
                         @Value("${rag.knowledge-path:/api/v1/rag/knowledge}") String knowledgePath,
                         @Value("${rag.knowledge-rebuild-path:/api/v1/rag/knowledge/rebuild}") String knowledgeRebuildPath,
                         @Value("${rag.top-k:4}") int topK,
                         @Value("${rag.timeout-seconds:5}") long timeoutSeconds) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.retrievePath = retrievePath;
        this.healthPath = healthPath;
        this.knowledgePath = knowledgePath;
        this.knowledgeRebuildPath = knowledgeRebuildPath;
        this.topK = topK;
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getRetrievePath() {
        return retrievePath;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public String getKnowledgePath() {
        return knowledgePath;
    }

    public String getKnowledgeRebuildPath() {
        return knowledgeRebuildPath;
    }

    public int getTopK() {
        return topK;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}


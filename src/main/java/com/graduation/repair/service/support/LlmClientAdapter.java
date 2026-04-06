package com.graduation.repair.service.support;

public interface LlmClientAdapter {

    LlmClientResponse chatJson(String systemPrompt, String userPrompt);

    String providerName();

    String modelName();
}

package com.graduation.repair.service.support;

public interface LlmClientAdapter {

    String clientKey();

    LlmClientResponse chatJson(String systemPrompt, String userPrompt);

    String providerName();

    String modelName();
}

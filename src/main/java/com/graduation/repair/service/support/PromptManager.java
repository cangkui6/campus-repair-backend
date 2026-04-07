package com.graduation.repair.service.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PromptManager {

    @Value("${llm.prompt.parse-version:parse-v1}")
    private String parsePromptVersion;

    @Value("${llm.prompt.classify-version:classify-v1}")
    private String classifyPromptVersion;

    public String parsePromptVersion() {
        return parsePromptVersion;
    }

    public String classifyPromptVersion() {
        return classifyPromptVersion;
    }

    public String parseSystemPrompt() {
        if ("parse-v2".equals(parsePromptVersion)) {
            return "你是校园报修工单解析助手。请输出 JSON，字段包括：category, location, faultPhenomenon, urgency, contact, timePreference, confidence。对无意义文本要将 confidence 设为 0.1 以下，location 留空。";
        }
        return "你是校园报修工单解析助手。请严格输出一个JSON对象，不要输出额外解释。字段包括：category, location, faultPhenomenon, urgency, contact, timePreference, confidence。category 只能是 WATER_ELECTRIC、NETWORK、FURNITURE、AIR_CONDITIONER、DOOR_WINDOW、LIGHTING、OTHER。urgency 只能是 LOW、MEDIUM、HIGH。confidence 取0到1之间数字。";
    }

    public String classifySystemPrompt() {
        return "你是校园报修分类助手。请输出一个 JSON，字段仅包含 category 与 confidence。";
    }

    public String parseUserPrompt(String rawText) {
        return "请解析以下报修文本：\n" + rawText;
    }

    public String classifyUserPrompt(String rawText) {
        return "请判断以下报修文本所属分类：\n" + rawText;
    }
}

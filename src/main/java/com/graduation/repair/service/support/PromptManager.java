package com.graduation.repair.service.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PromptManager {

    @Value("${llm.prompt.parse-version:parse-v1}")
    private String parsePromptVersion;

    public String parsePromptVersion() {
        return parsePromptVersion;
    }

    public String parseSystemPrompt() {
        return "你是校园报修工单解析助手。请严格输出一个JSON对象，不要输出额外解释。字段包括：category, location, faultPhenomenon, urgency, contact, timePreference, confidence。category 只能是 WATER_ELECTRIC、NETWORK、FURNITURE、AIR_CONDITIONER、DOOR_WINDOW、LIGHTING、OTHER。urgency 只能是 LOW、MEDIUM、HIGH。confidence 取0到1之间数字。";
    }

    public String parseUserPrompt(String rawText) {
        return "请解析以下报修文本：\n" + rawText;
    }
}

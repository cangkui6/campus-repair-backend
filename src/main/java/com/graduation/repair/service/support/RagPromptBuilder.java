package com.graduation.repair.service.support;

import com.graduation.repair.integration.rag.RagRetrieveItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagPromptBuilder {

    private static final int MAX_CHARS = 1800;

    public String buildParseUserPrompt(String rawText, List<RagRetrieveItem> items) {
        if (items == null || items.isEmpty()) {
            return "请解析以下报修文本：\n" + rawText;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("【检索到的领域知识】\n");
        int idx = 1;
        for (RagRetrieveItem item : items) {
            String block = "[知识" + idx + "][" + safe(item.getType()) + "]\n"
                    + safe(item.getContent()) + "\n";
            if (builder.length() + block.length() > MAX_CHARS) {
                break;
            }
            builder.append(block);
            idx++;
        }
        builder.append("【用户报修文本】\n").append(rawText).append("\n")
                .append("请结合上述知识，输出严格JSON，字段包括：category, location, faultPhenomenon, urgency, contact, timePreference, confidence。\n");
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

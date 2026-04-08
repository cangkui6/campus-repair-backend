package com.graduation.repair.service.support;

import com.graduation.repair.common.exception.BizException;

public enum ParseFailureReason {
    INVALID_JSON("INVALID_JSON", "模型返回 JSON 无法解析"),
    LOW_CONFIDENCE("LOW_CONFIDENCE", "模型置信度过低"),
    MEANINGLESS_TEXT("MEANINGLESS_TEXT", "文本缺乏有效故障语义"),
    EMPTY_FAULT("EMPTY_FAULT", "故障描述为空"),
    UNKNOWN_LOCATION_AND_OTHER_CATEGORY("UNKNOWN_LOCATION_AND_OTHER_CATEGORY", "位置未知且分类无法确定");

    private final String code;
    private final String message;

    ParseFailureReason(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public BizException toException() {
        return new BizException(4199, message);
    }
}

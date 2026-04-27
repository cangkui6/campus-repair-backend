package com.graduation.repair.service.support;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ParseResultValidator {

    private static final Set<String> SHORT_VALID_FAULTS = Set.of(
            "漏水", "断电", "停电", "异响", "堵塞", "跳闸", "闪烁", "掉线", "坏了", "损坏", "破损", "脱落", "卡住",
            "不亮", "不响", "不热", "不冷", "不通", "没网", "无网", "断网", "冒烟", "漏电", "渗水", "积水", "堵了"
    );
    private static final Set<String> INVALID_FAULT_TEXTS = Set.of(
            "无", "没有", "未知", "不知道", "不清楚", "随便", "测试", "test", "hello", "你好", "看看", "报修"
    );

    public void validateOrThrow(ParsedTicketData data) {
        if (data == null) {
            throw ParseFailureReason.EMPTY_FAULT.toException();
        }
        String fault = data.getFaultPhenomenon();
        if (fault == null || fault.isBlank()) {
            throw ParseFailureReason.EMPTY_FAULT.toException();
        }

        String trimmedFault = fault.trim();
        if (isInvalidFaultText(trimmedFault)) {
            throw ParseFailureReason.MEANINGLESS_TEXT.toException();
        }
        if (isRepeatingSingleChar(trimmedFault)) {
            throw ParseFailureReason.MEANINGLESS_TEXT.toException();
        }
        if (isTooShortAndNoContext(trimmedFault, data)) {
            throw ParseFailureReason.MEANINGLESS_TEXT.toException();
        }
        if ((data.getConfidence() == null ? 0.0 : data.getConfidence()) < 0.3) {
            throw ParseFailureReason.LOW_CONFIDENCE.toException();
        }
        if (isUnknownLocation(data.getLocation()) && isUnknownCategory(data.getCategory())) {
            throw ParseFailureReason.UNKNOWN_LOCATION_AND_OTHER_CATEGORY.toException();
        }
    }

    private boolean isTooShortAndNoContext(String fault, ParsedTicketData data) {
        if (fault.length() >= 2 && SHORT_VALID_FAULTS.contains(fault)) {
            return false;
        }
        if (fault.length() >= 2 && hasUsefulContext(data)) {
            return false;
        }
        return fault.length() < 2;
    }

    private boolean hasUsefulContext(ParsedTicketData data) {
        return !isUnknownCategory(data.getCategory()) || !isUnknownLocation(data.getLocation());
    }

    private boolean isInvalidFaultText(String text) {
        String normalized = text.toLowerCase();
        return INVALID_FAULT_TEXTS.contains(normalized);
    }

    private boolean isUnknownCategory(String category) {
        return category == null || category.isBlank() || "OTHER".equals(category.trim());
    }

    private boolean isUnknownLocation(String location) {
        return location == null || location.isBlank() || "未知位置".equals(location.trim());
    }

    private boolean isRepeatingSingleChar(String text) {
        if (text.length() < 4) {
            return false;
        }
        char first = text.charAt(0);
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }
}

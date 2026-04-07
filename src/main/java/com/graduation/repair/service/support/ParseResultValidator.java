package com.graduation.repair.service.support;

import org.springframework.stereotype.Component;

@Component
public class ParseResultValidator {

    public void validateOrThrow(ParsedTicketData data) {
        if (data == null) {
            throw ParseFailureReason.EMPTY_FAULT.toException();
        }
        String fault = data.getFaultPhenomenon();
        if (fault == null || fault.isBlank()) {
            throw ParseFailureReason.EMPTY_FAULT.toException();
        }

        String trimmedFault = fault.trim();
        if (trimmedFault.length() < 4) {
            throw ParseFailureReason.MEANINGLESS_TEXT.toException();
        }
        if (isRepeatingSingleChar(trimmedFault)) {
            throw ParseFailureReason.MEANINGLESS_TEXT.toException();
        }
        if ((data.getConfidence() == null ? 0.0 : data.getConfidence()) < 0.3) {
            throw ParseFailureReason.LOW_CONFIDENCE.toException();
        }
        if ("OTHER".equals(data.getCategory()) && "未知位置".equals(data.getLocation())) {
            throw ParseFailureReason.UNKNOWN_LOCATION_AND_OTHER_CATEGORY.toException();
        }
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

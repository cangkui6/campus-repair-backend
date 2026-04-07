package com.graduation.repair.service.support;

import org.springframework.stereotype.Component;

@Component
public class ParseResultValidator {

    public boolean isValid(ParsedTicketData data) {
        if (data == null) {
            return false;
        }
        String fault = data.getFaultPhenomenon();
        if (fault == null || fault.isBlank()) {
            return false;
        }

        String trimmedFault = fault.trim();
        if (trimmedFault.length() < 4) {
            return false;
        }
        if (isRepeatingSingleChar(trimmedFault)) {
            return false;
        }
        if ((data.getConfidence() == null ? 0.0 : data.getConfidence()) < 0.3) {
            return false;
        }
        return !"OTHER".equals(data.getCategory()) || !"未知位置".equals(data.getLocation());
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

package com.graduation.repair.service.support;

import org.springframework.stereotype.Component;

@Component
public class ParseResultValidator {

    public boolean isValid(ParsedTicketData data) {
        return data != null
                && data.getFaultPhenomenon() != null
                && !data.getFaultPhenomenon().isBlank();
    }
}

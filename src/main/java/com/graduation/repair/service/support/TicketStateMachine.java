package com.graduation.repair.service.support;

import com.graduation.repair.common.enums.TicketStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TicketStateMachine {

    private final Set<String> allowedTransitions = new HashSet<>();

    public TicketStateMachine() {
        allow(TicketStatus.PENDING, TicketStatus.PARSED);
        allow(TicketStatus.PENDING, TicketStatus.MANUAL_REVIEW);
        allow(TicketStatus.PENDING, TicketStatus.PENDING_DISPATCH);
        allow(TicketStatus.MANUAL_REVIEW, TicketStatus.PARSED);
        allow(TicketStatus.MANUAL_REVIEW, TicketStatus.PENDING_DISPATCH);
        allow(TicketStatus.PARSED, TicketStatus.PENDING_DISPATCH);
        allow(TicketStatus.PARSED, TicketStatus.DISPATCHED);
        allow(TicketStatus.PENDING_DISPATCH, TicketStatus.DISPATCHED);
        allow(TicketStatus.DISPATCHED, TicketStatus.PROCESSING);
        allow(TicketStatus.PROCESSING, TicketStatus.COMPLETED);
        allow(TicketStatus.COMPLETED, TicketStatus.EVALUATED);
        allow(TicketStatus.COMPLETED, TicketStatus.CLOSED);
        allow(TicketStatus.EVALUATED, TicketStatus.CLOSED);

        allow(TicketStatus.DISPATCHED, TicketStatus.PENDING_DISPATCH);
        allow(TicketStatus.DISPATCHED, TicketStatus.PARSED);
        allow(TicketStatus.PROCESSING, TicketStatus.DISPATCHED);
    }

    public boolean canTransit(String from, String to) {
        return allowedTransitions.contains(key(from, to));
    }

    private void allow(TicketStatus from, TicketStatus to) {
        allow(from.getValue(), to.getValue());
    }

    private void allow(String from, String to) {
        allowedTransitions.add(key(from, to));
    }

    private String key(String from, String to) {
        return from + "->" + to;
    }
}

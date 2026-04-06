package com.graduation.repair.service.support;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TicketNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private String currentDate = "";
    private final AtomicInteger sequence = new AtomicInteger(0);

    public synchronized String nextNo() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        if (!date.equals(currentDate)) {
            currentDate = date;
            sequence.set(0);
        }
        int seq = sequence.incrementAndGet();
        return "RP" + date + String.format("%04d", seq);
    }
}

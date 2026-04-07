package com.graduation.repair.service.support;

import com.graduation.repair.repository.RepairTicketRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TicketNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RepairTicketRepository repairTicketRepository;

    public TicketNoGenerator(RepairTicketRepository repairTicketRepository) {
        this.repairTicketRepository = repairTicketRepository;
    }

    public synchronized String nextNo() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String prefix = "RP" + date;
        int nextSeq = repairTicketRepository.findTopByTicketNoStartingWithOrderByTicketNoDesc(prefix)
                .map(ticket -> parseSeq(ticket.getTicketNo(), prefix) + 1)
                .orElse(1);
        return prefix + String.format("%04d", nextSeq);
    }

    private int parseSeq(String ticketNo, String prefix) {
        if (ticketNo == null || !ticketNo.startsWith(prefix) || ticketNo.length() <= prefix.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(ticketNo.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}

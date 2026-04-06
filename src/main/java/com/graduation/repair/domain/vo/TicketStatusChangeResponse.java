package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketStatusChangeResponse {

    private Long ticketId;
    private String fromStatus;
    private String toStatus;
}

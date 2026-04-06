package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketCreateResponse {

    private Long ticketId;
    private String ticketNo;
    private String status;
}

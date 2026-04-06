package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class TicketMyListItemVO {

    private Long ticketId;
    private String ticketNo;
    private String status;
    private String faultDesc;
    private String locationText;
    private LocalDateTime submittedAt;
}

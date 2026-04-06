package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class TicketDetailVO {

    private Long ticketId;
    private String ticketNo;
    private String rawText;
    private String locationText;
    private String faultDesc;
    private String urgencyLevel;
    private String status;
    private String contactMasked;
    private LocalDateTime submittedAt;
}

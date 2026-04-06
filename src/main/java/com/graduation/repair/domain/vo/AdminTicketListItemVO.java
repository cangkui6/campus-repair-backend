package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class AdminTicketListItemVO {

    private Long ticketId;
    private String ticketNo;
    private Long reporterId;
    private Long categoryId;
    private String status;
    private Long currentWorkerId;
    private String locationText;
    private String faultDesc;
    private String urgencyLevel;
    private LocalDateTime submittedAt;
}

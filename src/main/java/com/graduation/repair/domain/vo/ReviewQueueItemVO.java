package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ReviewQueueItemVO {

    private Long id;
    private Long ticketId;
    private String ticketNo;
    private String rawText;
    private String reasonCode;
    private String reason;
    private String queueStatus;
    private LocalDateTime createdAt;
}

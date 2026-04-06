package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class MessageListItemVO {

    private Long id;
    private Long ticketId;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
}

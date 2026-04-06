package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class DispatchResultVO {

    private Long ticketId;
    private Long selectedWorkerId;
    private String dispatchStatus;
    private String reason;
    private List<DispatchScoreVO> scores;
}

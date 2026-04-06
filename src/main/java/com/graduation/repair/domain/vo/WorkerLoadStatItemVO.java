package com.graduation.repair.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WorkerLoadStatItemVO {

    private Long workerId;
    private String workerName;
    private Long count;
}

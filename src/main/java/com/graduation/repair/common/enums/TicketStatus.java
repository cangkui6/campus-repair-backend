package com.graduation.repair.common.enums;

import lombok.Getter;

@Getter
public enum TicketStatus {
    PENDING("待受理"),
    MANUAL_REVIEW("待人工确认"),
    PARSED("已解析"),
    PENDING_DISPATCH("待分配"),
    DISPATCHED("已派单"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    EVALUATED("已评价"),
    CLOSED("已关闭");

    private final String value;

    TicketStatus(String value) {
        this.value = value;
    }
}

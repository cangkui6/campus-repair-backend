package com.graduation.repair.service.support;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TicketStateMachine {

    private final Set<String> allowedTransitions = new HashSet<>();

    public TicketStateMachine() {
        allow("待受理", "已解析");
        allow("已解析", "已派单");
        allow("已派单", "处理中");
        allow("处理中", "已完成");
        allow("已完成", "已评价");
        allow("已完成", "已关闭");
        allow("已评价", "已关闭");

        allow("已派单", "已解析");
        allow("处理中", "已派单");
    }

    public boolean canTransit(String from, String to) {
        return allowedTransitions.contains(key(from, to));
    }

    private void allow(String from, String to) {
        allowedTransitions.add(key(from, to));
    }

    private String key(String from, String to) {
        return from + "->" + to;
    }
}

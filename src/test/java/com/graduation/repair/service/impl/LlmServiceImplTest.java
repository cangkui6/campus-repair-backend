package com.graduation.repair.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.LlmParseRequest;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.repository.FaultCategoryRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.support.TicketStateMachine;
import com.graduation.repair.service.support.ZhipuLlmClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

class LlmServiceImplTest {

    private RepairTicketRepository repairTicketRepository;
    private TicketStateMachine ticketStateMachine;
    private LlmServiceImpl llmService;

    @BeforeEach
    void setUp() {
        repairTicketRepository = Mockito.mock(RepairTicketRepository.class);
        ticketStateMachine = Mockito.mock(TicketStateMachine.class);

        ZhipuLlmClient zhipuLlmClient = Mockito.mock(ZhipuLlmClient.class);
        Mockito.when(zhipuLlmClient.chatJson(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("{\"category\":\"NETWORK\",\"location\":\"教学楼\",\"faultPhenomenon\":\"网络掉线\",\"urgency\":\"HIGH\",\"confidence\":0.92}");

        llmService = new LlmServiceImpl(
                repairTicketRepository,
                Mockito.mock(FaultCategoryRepository.class),
                Mockito.mock(OperationLogRepository.class),
                ticketStateMachine,
                zhipuLlmClient,
                new ObjectMapper()
        );
    }

    @Test
    void parse_shouldRejectWhenStateTransitionIllegal() {
        RepairTicket ticket = new RepairTicket();
        ticket.setId(100L);
        ticket.setStatus("已关闭");
        ticket.setRawText("网络掉线");

        Mockito.when(repairTicketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        Mockito.when(ticketStateMachine.canTransit("已关闭", "已解析")).thenReturn(false);

        LlmParseRequest request = new LlmParseRequest();
        request.setTicketId(100L);

        BizException ex = Assertions.assertThrows(BizException.class,
                () -> llmService.parse(request, 1L));

        Assertions.assertEquals(4001, ex.getCode());
    }
}

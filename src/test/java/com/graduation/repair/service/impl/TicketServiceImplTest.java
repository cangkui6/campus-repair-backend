package com.graduation.repair.service.impl;

import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.TicketCreateRequest;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.NotificationService;
import com.graduation.repair.service.support.TicketNoGenerator;
import com.graduation.repair.service.support.TicketStateMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TicketServiceImplTest {

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                Mockito.mock(RepairTicketRepository.class),
                Mockito.mock(OperationLogRepository.class),
                Mockito.mock(TicketNoGenerator.class),
                Mockito.mock(TicketStateMachine.class),
                Mockito.mock(NotificationService.class)
        );
    }

    @Test
    void createTicket_shouldThrowWhenRawTextNull() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setRawText(null);
        request.setContactPhone("13900000000");

        BizException ex = Assertions.assertThrows(BizException.class,
                () -> ticketService.createTicket(1L, "REPORTER", request));

        Assertions.assertEquals(4002, ex.getCode());
    }
}

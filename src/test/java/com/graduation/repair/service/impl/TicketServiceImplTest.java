package com.graduation.repair.service.impl;

import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.TicketCreateRequest;
import com.graduation.repair.domain.entity.MaintenanceWorker;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.repository.LlmParseAuditLogRepository;
import com.graduation.repair.repository.MaintenanceWorkerRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.DispatchFeedbackService;
import com.graduation.repair.service.NotificationService;
import com.graduation.repair.service.support.TicketNoGenerator;
import com.graduation.repair.service.support.TicketStateMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

class TicketServiceImplTest {

    private RepairTicketRepository repairTicketRepository;
    private MaintenanceWorkerRepository maintenanceWorkerRepository;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        repairTicketRepository = Mockito.mock(RepairTicketRepository.class);
        maintenanceWorkerRepository = Mockito.mock(MaintenanceWorkerRepository.class);
        ticketService = new TicketServiceImpl(
                repairTicketRepository,
                Mockito.mock(OperationLogRepository.class),
                Mockito.mock(TicketNoGenerator.class),
                Mockito.mock(TicketStateMachine.class),
                Mockito.mock(DispatchFeedbackService.class),
                Mockito.mock(NotificationService.class),
                maintenanceWorkerRepository,
                Mockito.mock(LlmParseAuditLogRepository.class),
                new ObjectMapper()
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

    @Test
    void getTicketDetail_shouldRejectWorkerAccessingOtherWorkersTicket() {
        RepairTicket ticket = new RepairTicket();
        ticket.setId(100L);
        ticket.setReporterId(4L);
        ticket.setCurrentWorkerId(99L);
        ticket.setStatus("已派单");
        Mockito.when(repairTicketRepository.findById(100L)).thenReturn(Optional.of(ticket));

        MaintenanceWorker worker = new MaintenanceWorker();
        worker.setId(57L);
        worker.setUserId(2L);
        Mockito.when(maintenanceWorkerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));

        BizException ex = Assertions.assertThrows(BizException.class,
                () -> ticketService.getTicketDetail(2L, "WORKER", 100L));

        Assertions.assertEquals(4032, ex.getCode());
    }
}

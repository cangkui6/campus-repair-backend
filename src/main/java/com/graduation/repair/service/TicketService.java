package com.graduation.repair.service;

import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.TicketCompleteRequest;
import com.graduation.repair.domain.dto.TicketCreateRequest;
import com.graduation.repair.domain.dto.TicketEvaluateRequest;
import com.graduation.repair.domain.vo.TicketCreateResponse;
import com.graduation.repair.domain.vo.TicketDetailVO;
import com.graduation.repair.domain.vo.TicketMyListItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;

public interface TicketService {

    TicketCreateResponse createTicket(Long reporterId, String role, TicketCreateRequest request);

    TicketDetailVO getTicketDetail(Long userId, String role, Long ticketId);

    PageResult<TicketMyListItemVO> myTickets(Long reporterId, String role, Integer page, Integer size, String status);

    TicketStatusChangeResponse acceptTicket(Long operatorId, String role, Long ticketId);

    TicketStatusChangeResponse completeTicket(Long operatorId, String role, Long ticketId, TicketCompleteRequest request);

    TicketStatusChangeResponse evaluateTicket(Long operatorId, String role, Long ticketId, TicketEvaluateRequest request);

    TicketStatusChangeResponse closeTicket(Long operatorId, String role, Long ticketId);
}

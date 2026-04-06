package com.graduation.repair.service;

import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.AdminCategoryCorrectRequest;
import com.graduation.repair.domain.dto.AdminRollbackRequest;
import com.graduation.repair.domain.vo.AdminTicketListItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;

public interface AdminTicketService {

    PageResult<AdminTicketListItemVO> listTickets(Integer page, Integer size, String status, Long categoryId);

    TicketStatusChangeResponse rollbackTicket(Long operatorId, Long ticketId, AdminRollbackRequest request);

    TicketStatusChangeResponse correctCategory(Long operatorId, Long ticketId, AdminCategoryCorrectRequest request);

    void deleteTicket(Long operatorId, Long ticketId);
}

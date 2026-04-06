package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.AdminCategoryCorrectRequest;
import com.graduation.repair.domain.dto.AdminRollbackRequest;
import com.graduation.repair.domain.vo.AdminTicketListItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.AdminTicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    public AdminTicketController(AdminTicketService adminTicketService) {
        this.adminTicketService = adminTicketService;
    }

    @GetMapping("/tickets")
    public ApiResponse<PageResult<AdminTicketListItemVO>> listTickets(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long category
    ) {
        return ApiResponse.success(adminTicketService.listTickets(page, size, status, category));
    }

    @PostMapping("/tickets/{ticketId}/rollback")
    public ApiResponse<TicketStatusChangeResponse> rollback(
            @PathVariable Long ticketId,
            @Valid @RequestBody AdminRollbackRequest request
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(adminTicketService.rollbackTicket(user.getUserId(), ticketId, request));
    }

    @PostMapping("/tickets/{ticketId}/category")
    public ApiResponse<TicketStatusChangeResponse> correctCategory(
            @PathVariable Long ticketId,
            @Valid @RequestBody AdminCategoryCorrectRequest request
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(adminTicketService.correctCategory(user.getUserId(), ticketId, request));
    }

    @PostMapping("/tickets/{ticketId}/delete")
    public ApiResponse<Void> deleteTicket(@PathVariable Long ticketId) {
        AuthUser user = SecurityUserContext.currentUser();
        adminTicketService.deleteTicket(user.getUserId(), ticketId);
        return ApiResponse.success(null);
    }
}

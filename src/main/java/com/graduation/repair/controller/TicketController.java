package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.dto.TicketCompleteRequest;
import com.graduation.repair.domain.dto.TicketCreateRequest;
import com.graduation.repair.domain.dto.TicketEvaluateRequest;
import com.graduation.repair.domain.vo.TicketCreateResponse;
import com.graduation.repair.domain.vo.TicketDetailVO;
import com.graduation.repair.domain.vo.TicketMyListItemVO;
import com.graduation.repair.domain.vo.TicketStatusChangeResponse;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ApiResponse<TicketCreateResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(ticketService.createTicket(user.getUserId(), user.getRole(), request));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailVO> ticketDetail(@PathVariable Long ticketId) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(ticketService.getTicketDetail(user.getUserId(), user.getRole(), ticketId));
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<TicketMyListItemVO>> myTickets(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String status
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(ticketService.myTickets(user.getUserId(), user.getRole(), page, size, status));
    }

    @PostMapping("/{ticketId}/accept")
    public ApiResponse<TicketStatusChangeResponse> accept(@PathVariable Long ticketId) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(ticketService.acceptTicket(user.getUserId(), user.getRole(), ticketId));
    }

    @PostMapping("/{ticketId}/complete")
    public ApiResponse<TicketStatusChangeResponse> complete(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketCompleteRequest request
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(ticketService.completeTicket(user.getUserId(), user.getRole(), ticketId, request));
    }

    @PostMapping("/{ticketId}/evaluate")
    public ApiResponse<TicketStatusChangeResponse> evaluate(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketEvaluateRequest request
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(ticketService.evaluateTicket(user.getUserId(), user.getRole(), ticketId, request));
    }

    @PostMapping("/{ticketId}/close")
    public ApiResponse<TicketStatusChangeResponse> close(@PathVariable Long ticketId) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(ticketService.closeTicket(user.getUserId(), user.getRole(), ticketId));
    }
}

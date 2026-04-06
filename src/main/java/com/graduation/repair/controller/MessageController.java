package com.graduation.repair.controller;

import com.graduation.repair.common.api.ApiResponse;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.vo.MessageListItemVO;
import com.graduation.repair.security.AuthUser;
import com.graduation.repair.security.SecurityUserContext;
import com.graduation.repair.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final NotificationService notificationService;

    public MessageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResult<MessageListItemVO>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(notificationService.myMessages(user.getUserId(), page, size));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable("id") Long id) {
        AuthUser user = SecurityUserContext.currentUser();
        notificationService.markRead(user.getUserId(), id);
        return ApiResponse.success(null);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        AuthUser user = SecurityUserContext.currentUser();
        return ApiResponse.success(Map.of("unreadCount", notificationService.unreadCount(user.getUserId())));
    }
}

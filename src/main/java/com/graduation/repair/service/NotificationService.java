package com.graduation.repair.service;

import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.vo.MessageListItemVO;

public interface NotificationService {

    void notifyUser(Long receiverId, Long ticketId, String title, String content);

    PageResult<MessageListItemVO> myMessages(Long userId, Integer page, Integer size);

    void markRead(Long userId, Long messageId);

    long unreadCount(Long userId);
}

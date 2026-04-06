package com.graduation.repair.service.impl;

import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.entity.NotificationMessage;
import com.graduation.repair.domain.vo.MessageListItemVO;
import com.graduation.repair.repository.NotificationMessageRepository;
import com.graduation.repair.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMessageRepository notificationMessageRepository;

    public NotificationServiceImpl(NotificationMessageRepository notificationMessageRepository) {
        this.notificationMessageRepository = notificationMessageRepository;
    }

    @Override
    public void notifyUser(Long receiverId, Long ticketId, String title, String content) {
        NotificationMessage message = new NotificationMessage();
        message.setReceiverId(receiverId);
        message.setTicketId(ticketId);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());
        notificationMessageRepository.save(message);
    }

    @Override
    public PageResult<MessageListItemVO> myMessages(Long userId, Integer page, Integer size) {
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);

        Page<NotificationMessage> messagePage = notificationMessageRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);

        List<MessageListItemVO> records = messagePage.getContent().stream().map(item ->
                MessageListItemVO.builder()
                        .id(item.getId())
                        .ticketId(item.getTicketId())
                        .title(item.getTitle())
                        .content(item.getContent())
                        .isRead(item.getIsRead())
                        .createdAt(item.getCreatedAt())
                        .build()).toList();

        return new PageResult<>(messagePage.getTotalElements(), pageNo, pageSize, records);
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        NotificationMessage message = notificationMessageRepository.findById(messageId)
                .orElseThrow(() -> new BizException(4044, "消息不存在"));
        if (!message.getReceiverId().equals(userId)) {
            throw new BizException(4037, "无权限操作该消息");
        }
        message.setIsRead(1);
        notificationMessageRepository.save(message);
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMessageRepository.countByReceiverIdAndIsRead(userId, 0);
    }
}

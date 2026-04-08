package com.graduation.repair.service;

import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.vo.WorkerHistoryItemVO;
import com.graduation.repair.domain.vo.WorkerProfileVO;
import com.graduation.repair.domain.vo.WorkerTaskListItemVO;

public interface WorkerTaskService {

    PageResult<WorkerTaskListItemVO> myTasks(Long userId, String role, Integer page, Integer size, String status);

    PageResult<WorkerTaskListItemVO> availableTasks(Long userId, String role, Integer page, Integer size);

    PageResult<WorkerHistoryItemVO> myHistory(Long userId, String role, Integer page, Integer size);

    WorkerProfileVO myProfile(Long userId, String role);

    void grabTask(Long userId, String role, Long ticketId);

    void rejectTask(Long userId, String role, Long ticketId, String reason);
}

package com.graduation.repair.service;

import com.graduation.repair.common.pagination.PageResult;
import com.graduation.repair.domain.vo.WorkerTaskListItemVO;

public interface WorkerTaskService {

    PageResult<WorkerTaskListItemVO> myTasks(Long userId, String role, Integer page, Integer size, String status);
}

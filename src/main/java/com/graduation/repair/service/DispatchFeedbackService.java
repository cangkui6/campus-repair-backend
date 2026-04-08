package com.graduation.repair.service;

import com.graduation.repair.domain.dto.DispatchWeightUpdateRequest;
import com.graduation.repair.domain.vo.DispatchFeedbackOverviewVO;

public interface DispatchFeedbackService {

    DispatchFeedbackOverviewVO overview();

    DispatchFeedbackOverviewVO recalculate(Long operatorId);

    DispatchFeedbackOverviewVO updateWeights(Long operatorId, DispatchWeightUpdateRequest request);

    void onTicketCompleted(Long operatorId, Long ticketId);
}

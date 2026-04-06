package com.graduation.repair.service;

import com.graduation.repair.domain.dto.DispatchManualRequest;
import com.graduation.repair.domain.dto.DispatchRetryRequest;
import com.graduation.repair.domain.vo.DispatchResultVO;

public interface DispatchService {

    DispatchResultVO autoDispatch(Long operatorId, String role, Long ticketId);

    DispatchResultVO manualDispatch(Long operatorId, String role, Long ticketId, DispatchManualRequest request);

    DispatchResultVO retryDispatch(Long operatorId, String role, Long ticketId, DispatchRetryRequest request);
}

package com.graduation.repair.service;

import com.graduation.repair.domain.dto.LlmClassifyRequest;
import com.graduation.repair.domain.dto.LlmParseRequest;
import com.graduation.repair.domain.vo.LlmClassifyResponse;
import com.graduation.repair.domain.vo.LlmHealthVO;
import com.graduation.repair.domain.vo.LlmParseResponse;

public interface LlmService {

    LlmParseResponse parse(LlmParseRequest request, Long operatorId);

    LlmClassifyResponse classify(LlmClassifyRequest request);

    LlmHealthVO health();
}

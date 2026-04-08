package com.graduation.repair.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.repair.common.enums.TicketStatus;
import com.graduation.repair.common.exception.BizException;
import com.graduation.repair.domain.dto.LlmClassifyRequest;
import com.graduation.repair.domain.dto.LlmParseRequest;
import com.graduation.repair.domain.entity.FaultCategory;
import com.graduation.repair.domain.entity.OperationLog;
import com.graduation.repair.domain.entity.RepairTicket;
import com.graduation.repair.domain.vo.LlmClassifyResponse;
import com.graduation.repair.domain.vo.LlmHealthVO;
import com.graduation.repair.domain.vo.LlmParseResponse;
import com.graduation.repair.repository.FaultCategoryRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.LlmService;
import com.graduation.repair.service.support.LlmClientAdapter;
import com.graduation.repair.service.support.LlmClientResponse;
import com.graduation.repair.service.support.ParseAuditLogService;
import com.graduation.repair.service.support.ParseFailureReason;
import com.graduation.repair.service.support.ParseFallbackHandler;
import com.graduation.repair.service.support.ParseResultValidator;
import com.graduation.repair.service.support.ParsedTicketData;
import com.graduation.repair.service.support.PromptManager;
import com.graduation.repair.service.support.TicketStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class LlmServiceImpl implements LlmService {

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "WATER_ELECTRIC", "NETWORK", "FURNITURE", "AIR_CONDITIONER", "DOOR_WINDOW", "LIGHTING", "OTHER"
    );
    private static final Set<String> VALID_URGENCY = Set.of("LOW", "MEDIUM", "HIGH");

    private final RepairTicketRepository repairTicketRepository;
    private final FaultCategoryRepository faultCategoryRepository;
    private final OperationLogRepository operationLogRepository;
    private final TicketStateMachine ticketStateMachine;
    private final LlmClientAdapter llmClientAdapter;
    private final ObjectMapper objectMapper;
    private final PromptManager promptManager;
    private final ParseResultValidator parseResultValidator;
    private final ParseFallbackHandler parseFallbackHandler;
    private final ParseAuditLogService parseAuditLogService;

    public LlmServiceImpl(RepairTicketRepository repairTicketRepository,
                          FaultCategoryRepository faultCategoryRepository,
                          OperationLogRepository operationLogRepository,
                          TicketStateMachine ticketStateMachine,
                          LlmClientAdapter llmClientAdapter,
                          ObjectMapper objectMapper,
                          PromptManager promptManager,
                          ParseResultValidator parseResultValidator,
                          ParseFallbackHandler parseFallbackHandler,
                          ParseAuditLogService parseAuditLogService) {
        this.repairTicketRepository = repairTicketRepository;
        this.faultCategoryRepository = faultCategoryRepository;
        this.operationLogRepository = operationLogRepository;
        this.ticketStateMachine = ticketStateMachine;
        this.llmClientAdapter = llmClientAdapter;
        this.objectMapper = objectMapper;
        this.promptManager = promptManager;
        this.parseResultValidator = parseResultValidator;
        this.parseFallbackHandler = parseFallbackHandler;
        this.parseAuditLogService = parseAuditLogService;
    }

    @Override
    @Transactional
    public LlmParseResponse parse(LlmParseRequest request, Long operatorId) {
        if (request.getTicketId() == null && (request.getRawText() == null || request.getRawText().isBlank())) {
            throw new BizException(4103, "ticketId与rawText不能同时为空");
        }

        RepairTicket ticket = null;
        String rawText = request.getRawText();
        if (request.getTicketId() != null) {
            ticket = repairTicketRepository.findById(request.getTicketId())
                    .orElseThrow(() -> new BizException(4041, "工单不存在"));
            if (rawText == null || rawText.isBlank()) {
                rawText = ticket.getRawText();
            }
        }

        String promptVersion = promptManager.parsePromptVersion();
        LlmClientResponse clientResponse = llmClientAdapter.chatJson(
                promptManager.parseSystemPrompt(),
                promptManager.parseUserPrompt(rawText)
        );

        ParsedTicketData normalized;
        String normalizedJson;
        try {
            normalized = normalize(parseRawJson(clientResponse.getRawResponse()));
            normalizedJson = toJson(normalized);
            parseResultValidator.validateOrThrow(normalized);
        } catch (BizException ex) {
            ParseFailureReason reason = mapFailureReason(ex.getMessage());
            if (ticket != null) {
                String fromStatus = ticket.getStatus();
                if (ticketStateMachine.canTransit(fromStatus, TicketStatus.MANUAL_REVIEW.getValue())) {
                    ticket.setStatus(TicketStatus.MANUAL_REVIEW.getValue());
                    ticket.setUpdatedAt(LocalDateTime.now());
                    repairTicketRepository.save(ticket);
                }
                writeParseLog(ticket.getId(), operatorId, fromStatus, TicketStatus.MANUAL_REVIEW.getValue(), "LLM解析异常，已转人工确认");
            }
            parseFallbackHandler.enqueue(request.getTicketId(), operatorId, rawText, reason);
            parseAuditLogService.save(request.getTicketId(), operatorId, promptVersion, llmClientAdapter.providerName(), llmClientAdapter.modelName(), clientResponse.getLatencyMs(), clientResponse.getRawResponse(), "FAILED_NEED_MANUAL_REVIEW", "{}");
            throw new BizException(4199, "llm parse failed, fallback to manual review");
        }

        if (ticket != null) {
            String fromStatus = ticket.getStatus();
            if (!"待受理".equals(fromStatus)) {
                throw new BizException(4001, "仅待受理工单允许执行LLM解析");
            }
            ticket.setLocationText(normalized.getLocation());
            ticket.setFaultDesc(normalized.getFaultPhenomenon());
            ticket.setUrgencyLevel(normalized.getUrgency());
            if (normalized.getContact() != null && !normalized.getContact().isBlank()) {
                ticket.setContactMasked(normalized.getContact());
            }
            ticket.setTimePreference(normalized.getTimePreference());
            ticket.setCategoryId(resolveCategoryId(normalized.getCategory()));
            ticket.setStatus(TicketStatus.PENDING_DISPATCH.getValue());
            ticket.setUpdatedAt(LocalDateTime.now());
            repairTicketRepository.save(ticket);
            writeParseLog(ticket.getId(), operatorId, fromStatus, TicketStatus.PENDING_DISPATCH.getValue(), "LLM解析成功，已进入待分配池");
        }

        parseAuditLogService.save(request.getTicketId(), operatorId, promptVersion, llmClientAdapter.providerName(), llmClientAdapter.modelName(), clientResponse.getLatencyMs(), clientResponse.getRawResponse(), "SUCCESS", normalizedJson);

        return LlmParseResponse.builder()
                .category(normalized.getCategory())
                .location(normalized.getLocation())
                .faultPhenomenon(normalized.getFaultPhenomenon())
                .urgency(normalized.getUrgency())
                .contact(normalized.getContact())
                .timePreference(normalized.getTimePreference())
                .confidence(normalized.getConfidence())
                .parseStatus("SUCCESS")
                .promptVersion(promptVersion)
                .providerName(llmClientAdapter.providerName())
                .modelName(llmClientAdapter.modelName())
                .latencyMs(clientResponse.getLatencyMs())
                .fallbackQueued(false)
                .build();
    }

    @Override
    public LlmClassifyResponse classify(LlmClassifyRequest request) {
        LlmClientResponse clientResponse = llmClientAdapter.chatJson(
                promptManager.classifySystemPrompt(),
                promptManager.classifyUserPrompt(request.getRawText())
        );
        ParsedTicketData normalized = normalize(parseRawJson(clientResponse.getRawResponse()));
        return new LlmClassifyResponse(normalized.getCategory(), normalized.getConfidence() == null ? 0.9 : normalized.getConfidence());
    }

    @Override
    public LlmHealthVO health() {
        return LlmHealthVO.builder()
                .provider(llmClientAdapter.providerName() + ":" + llmClientAdapter.modelName())
                .status("UP")
                .latencyMs(300L)
                .build();
    }

    private ParsedTicketData parseRawJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return ParsedTicketData.builder()
                    .category(text(root, "category"))
                    .location(text(root, "location"))
                    .faultPhenomenon(text(root, "faultPhenomenon"))
                    .urgency(text(root, "urgency"))
                    .contact(text(root, "contact"))
                    .timePreference(text(root, "timePreference"))
                    .confidence(number(root, "confidence"))
                    .build();
        } catch (Exception e) {
            throw ParseFailureReason.INVALID_JSON.toException();
        }
    }

    private ParsedTicketData normalize(ParsedTicketData data) {
        return ParsedTicketData.builder()
                .category(normalizeCategory(data.getCategory()))
                .urgency(normalizeUrgency(data.getUrgency()))
                .location((data.getLocation() == null || data.getLocation().isBlank()) ? "未知位置" : data.getLocation().trim())
                .faultPhenomenon(data.getFaultPhenomenon() == null ? null : data.getFaultPhenomenon().trim())
                .contact(blankToNull(data.getContact()))
                .timePreference(blankToNull(data.getTimePreference()))
                .confidence(normalizeConfidence(data.getConfidence()))
                .build();
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "OTHER";
        }
        String value = category.trim().toUpperCase(Locale.ROOT);
        return VALID_CATEGORIES.contains(value) ? value : "OTHER";
    }

    private String normalizeUrgency(String urgency) {
        if (urgency == null) {
            return "MEDIUM";
        }
        String value = urgency.trim().toUpperCase(Locale.ROOT);
        return VALID_URGENCY.contains(value) ? value : "MEDIUM";
    }

    private Double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.9;
        }
        if (confidence < 0) {
            return 0.0;
        }
        if (confidence > 1) {
            return 1.0;
        }
        return round2(confidence);
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private Double number(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isNumber() ? node.asDouble() : null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long resolveCategoryId(String categoryCode) {
        return faultCategoryRepository.findByCategoryCode(categoryCode)
                .map(FaultCategory::getId)
                .orElse(null);
    }

    private void writeParseLog(Long ticketId, Long operatorId, String fromStatus, String toStatus, String detail) {
        if (operatorId == null) {
            throw new BizException(4010, "未登录或登录已过期");
        }
        OperationLog log = new OperationLog();
        log.setTicketId(ticketId);
        log.setOperatorId(operatorId);
        log.setAction("PARSE");
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    private String toJson(ParsedTicketData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private ParseFailureReason mapFailureReason(String message) {
        if (message == null) {
            return ParseFailureReason.EMPTY_FAULT;
        }
        for (ParseFailureReason value : ParseFailureReason.values()) {
            if (message.contains(value.message())) {
                return value;
            }
        }
        return ParseFailureReason.EMPTY_FAULT;
    }

    private Double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

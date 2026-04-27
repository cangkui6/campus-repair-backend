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
import com.graduation.repair.integration.rag.RagClient;
import com.graduation.repair.integration.rag.RagRetrieveItem;
import com.graduation.repair.repository.FaultCategoryRepository;
import com.graduation.repair.repository.OperationLogRepository;
import com.graduation.repair.repository.RepairTicketRepository;
import com.graduation.repair.service.LlmService;
import com.graduation.repair.service.support.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LlmServiceImpl implements LlmService {
    private static final Logger log= LoggerFactory.getLogger(LlmServiceImpl.class);
    private static final Set<String> VALID_CATEGORIES=Set.of("WATER_ELECTRIC","NETWORK","FURNITURE","AIR_CONDITIONER","DOOR_WINDOW","LIGHTING","OTHER");
    private static final Set<String> VALID_URGENCY=Set.of("LOW","MEDIUM","HIGH");
    private final RepairTicketRepository repairTicketRepository; private final FaultCategoryRepository faultCategoryRepository; private final OperationLogRepository operationLogRepository; private final TicketStateMachine ticketStateMachine; private final LlmClientAdapter llmClientAdapter; private final ObjectMapper objectMapper; private final PromptManager promptManager; private final ParseResultValidator parseResultValidator; private final ParseFallbackHandler parseFallbackHandler; private final ParseAuditLogService parseAuditLogService; private final RagClient ragClient; private final RagPromptBuilder ragPromptBuilder;
    public LlmServiceImpl(RepairTicketRepository repairTicketRepository, FaultCategoryRepository faultCategoryRepository, OperationLogRepository operationLogRepository, TicketStateMachine ticketStateMachine, LlmClientAdapter llmClientAdapter, ObjectMapper objectMapper, PromptManager promptManager, ParseResultValidator parseResultValidator, ParseFallbackHandler parseFallbackHandler, ParseAuditLogService parseAuditLogService, RagClient ragClient, RagPromptBuilder ragPromptBuilder) { this.repairTicketRepository=repairTicketRepository; this.faultCategoryRepository=faultCategoryRepository; this.operationLogRepository=operationLogRepository; this.ticketStateMachine=ticketStateMachine; this.llmClientAdapter=llmClientAdapter; this.objectMapper=objectMapper; this.promptManager=promptManager; this.parseResultValidator=parseResultValidator; this.parseFallbackHandler=parseFallbackHandler; this.parseAuditLogService=parseAuditLogService; this.ragClient=ragClient; this.ragPromptBuilder=ragPromptBuilder; }

    @Override @Transactional
    public LlmParseResponse parse(LlmParseRequest request, Long operatorId) {
        if (request.getTicketId()==null&&(request.getRawText()==null||request.getRawText().isBlank())) throw new BizException(4103,"ticketId与rawText不能同时为空");
        RepairTicket ticket=null; String rawText=request.getRawText();
        if (request.getTicketId()!=null) { ticket=repairTicketRepository.findById(request.getTicketId()).orElseThrow(() -> new BizException(4041,"工单不存在")); if (rawText==null||rawText.isBlank()) rawText=ticket.getRawText(); }
        boolean ragApplied=false; List<RagRetrieveItem> ragItems=List.of(); String systemPrompt=promptManager.parseSystemPrompt(); String userPrompt=promptManager.parseUserPrompt(rawText); String promptVersion=promptManager.parsePromptVersion();
        if (ragClient.isEnabled()) {
            try { ragItems=ragClient.retrieve(rawText,ragClient.topK()); if (!ragItems.isEmpty()) { ragApplied=true; systemPrompt=promptManager.parseSystemPrompt(true); userPrompt=ragPromptBuilder.buildParseUserPrompt(rawText,ragItems); promptVersion=promptManager.parsePromptVersion(true); } }
            catch (Exception ex) { log.warn("RAG retrieve failed, degrade to prompt-only flow. ticketId={}, reason={}", request.getTicketId(), ex.getMessage()); }
        }
        LlmClientResponse clientResponse=llmClientAdapter.chatJson(systemPrompt,userPrompt);
        ParsedTicketData normalized; String normalizedJson;
        try { normalized=normalize(parseRawJson(clientResponse.getRawResponse())); normalizedJson=buildAuditPayload(normalized,ragApplied,ragItems); parseResultValidator.validateOrThrow(normalized); }
        catch (BizException ex) {
            ParseFailureReason reason=mapFailureReason(ex.getMessage());
            if (ticket!=null) { String fromStatus=ticket.getStatus(); if (ticketStateMachine.canTransit(fromStatus, TicketStatus.MANUAL_REVIEW.getValue())) { ticket.setStatus(TicketStatus.MANUAL_REVIEW.getValue()); ticket.setUpdatedAt(LocalDateTime.now()); repairTicketRepository.save(ticket);} writeParseLog(ticket.getId(),operatorId,fromStatus,TicketStatus.MANUAL_REVIEW.getValue(),"LLM解析异常，已转人工确认"); }
            parseFallbackHandler.enqueue(request.getTicketId(),operatorId,rawText,reason);
            parseAuditLogService.save(request.getTicketId(),operatorId,promptVersion,llmClientAdapter.providerName(),llmClientAdapter.modelName(),clientResponse.getLatencyMs(),clientResponse.getRawResponse(),"FAILED_NEED_MANUAL_REVIEW",buildFailureAuditPayload(reason,ragApplied,ragItems));
            throw new BizException(4199,"llm parse failed, fallback to manual review");
        }
        if (ticket!=null) {
            String fromStatus=ticket.getStatus();
            if (!TicketStatus.PENDING.getValue().equals(fromStatus) && !TicketStatus.MANUAL_REVIEW.getValue().equals(fromStatus)) throw new BizException(4001,"仅待受理或人工复核工单允许执行LLM解析");
            ticket.setLocationText(normalized.getLocation()); ticket.setFaultDesc(normalized.getFaultPhenomenon()); ticket.setUrgencyLevel(normalized.getUrgency()); if (normalized.getContact()!=null&&!normalized.getContact().isBlank()) ticket.setContactMasked(normalized.getContact()); ticket.setTimePreference(normalized.getTimePreference()); ticket.setCategoryId(resolveCategoryId(normalized.getCategory())); ticket.setStatus(TicketStatus.PENDING_DISPATCH.getValue()); ticket.setUpdatedAt(LocalDateTime.now()); repairTicketRepository.save(ticket); writeParseLog(ticket.getId(),operatorId,fromStatus,TicketStatus.PENDING_DISPATCH.getValue(),"LLM解析成功，已进入待分配池");
        }
        parseAuditLogService.save(request.getTicketId(),operatorId,promptVersion,llmClientAdapter.providerName(),llmClientAdapter.modelName(),clientResponse.getLatencyMs(),clientResponse.getRawResponse(),"SUCCESS",normalizedJson);
        return LlmParseResponse.builder().category(normalized.getCategory()).location(normalized.getLocation()).faultPhenomenon(normalized.getFaultPhenomenon()).urgency(normalized.getUrgency()).contact(normalized.getContact()).timePreference(normalized.getTimePreference()).confidence(normalized.getConfidence()).parseStatus("SUCCESS").promptVersion(promptVersion).providerName(llmClientAdapter.providerName()).modelName(llmClientAdapter.modelName()).latencyMs(clientResponse.getLatencyMs()).fallbackQueued(false).ragEnabled(ragApplied).ragHitCount(ragItems.size()).build();
    }

    @Override public LlmClassifyResponse classify(LlmClassifyRequest request) { LlmClientResponse clientResponse=llmClientAdapter.chatJson(promptManager.classifySystemPrompt(),promptManager.classifyUserPrompt(request.getRawText())); ParsedTicketData normalized=normalize(parseRawJson(clientResponse.getRawResponse())); return new LlmClassifyResponse(normalized.getCategory(),normalized.getConfidence()==null?0.9:normalized.getConfidence()); }
    @Override public LlmHealthVO health() { return LlmHealthVO.builder().provider(llmClientAdapter.providerName()+":"+llmClientAdapter.modelName()).status("UP").latencyMs(300L).build(); }

    private ParsedTicketData parseRawJson(String json) { try { JsonNode root=objectMapper.readTree(json); return ParsedTicketData.builder().category(text(root,"category")).location(text(root,"location")).faultPhenomenon(text(root,"faultPhenomenon")).urgency(text(root,"urgency")).contact(text(root,"contact")).timePreference(text(root,"timePreference")).confidence(number(root,"confidence")).build(); } catch (Exception e) { throw ParseFailureReason.INVALID_JSON.toException(); } }
    private ParsedTicketData normalize(ParsedTicketData data) { return ParsedTicketData.builder().category(normalizeCategory(data.getCategory())).urgency(normalizeUrgency(data.getUrgency())).location((data.getLocation()==null||data.getLocation().isBlank())?"未知位置":data.getLocation().trim()).faultPhenomenon(data.getFaultPhenomenon()==null?null:data.getFaultPhenomenon().trim()).contact(blankToNull(data.getContact())).timePreference(blankToNull(data.getTimePreference())).confidence(normalizeConfidence(data.getConfidence())).build(); }
    private String normalizeCategory(String category) { if (category==null) return "OTHER"; String raw=category.trim(); String value=raw.toUpperCase(Locale.ROOT); if (VALID_CATEGORIES.contains(value)) return value; return switch (raw) { case "水电", "水电故障", "电路", "用电", "给排水", "水管", "插座" -> "WATER_ELECTRIC"; case "网络", "网络故障", "网线", "宽带", "无线网", "校园网" -> "NETWORK"; case "家具", "家具故障", "桌椅", "床", "柜子" -> "FURNITURE"; case "空调", "空调故障" -> "AIR_CONDITIONER"; case "门窗", "门窗故障", "门锁", "窗户" -> "DOOR_WINDOW"; case "照明", "照明故障", "灯", "灯具" -> "LIGHTING"; default -> "OTHER"; }; }
    private String normalizeUrgency(String urgency) { if (urgency==null) return "MEDIUM"; String raw=urgency.trim(); String value=raw.toUpperCase(Locale.ROOT); if (VALID_URGENCY.contains(value)) return value; if (raw.contains("马上")||raw.contains("立刻")||raw.contains("尽快")||raw.contains("紧急")||raw.contains("严重")||raw.contains("必须")||raw.contains("影响很大")) return "HIGH"; if (raw.contains("不急")||raw.contains("有空")||raw.contains("方便时")||raw.contains("低")) return "LOW"; return "MEDIUM"; }
    private Double normalizeConfidence(Double confidence) { if (confidence==null) return 0.9; if (confidence<0) return 0.0; if (confidence>1) return 1.0; return round2(confidence); }
    private String text(JsonNode root, String field) { JsonNode node=root.path(field); return node.isMissingNode()||node.isNull()?null:node.asText(); }
    private Double number(JsonNode root, String field) { JsonNode node=root.path(field); return node.isNumber()?node.asDouble():null; }
    private String blankToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private Long resolveCategoryId(String categoryCode) { return faultCategoryRepository.findByCategoryCode(categoryCode).map(FaultCategory::getId).orElse(null); }
    private void writeParseLog(Long ticketId, Long operatorId, String fromStatus, String toStatus, String detail) { if (operatorId==null) throw new BizException(4010,"未登录或登录已过期"); OperationLog logItem=new OperationLog(); logItem.setTicketId(ticketId); logItem.setOperatorId(operatorId); logItem.setAction("PARSE"); logItem.setFromStatus(fromStatus); logItem.setToStatus(toStatus); logItem.setDetail(detail); logItem.setCreatedAt(LocalDateTime.now()); operationLogRepository.save(logItem); }
    private String buildAuditPayload(ParsedTicketData data, boolean ragApplied, List<RagRetrieveItem> ragItems) { try { Map<String,Object> wrapper=new LinkedHashMap<>(); wrapper.put("parsed",data); wrapper.put("ragEnabled",ragApplied); wrapper.put("ragHitCount",ragItems==null?0:ragItems.size()); wrapper.put("ragHitIds",ragItems==null?List.of():ragItems.stream().map(RagRetrieveItem::getId).toList()); return objectMapper.writeValueAsString(wrapper); } catch (Exception e) { return "{}"; } }
    private String buildFailureAuditPayload(ParseFailureReason reason, boolean ragApplied, List<RagRetrieveItem> ragItems) { try { Map<String,Object> wrapper=new LinkedHashMap<>(); wrapper.put("parsed",Map.of()); wrapper.put("failureReason",reason.message()); wrapper.put("reasonCode",reason.name()); wrapper.put("ragEnabled",ragApplied); wrapper.put("ragHitCount",ragItems==null?0:ragItems.size()); wrapper.put("ragHitIds",ragItems==null?List.of():ragItems.stream().map(RagRetrieveItem::getId).toList()); return objectMapper.writeValueAsString(wrapper); } catch (Exception e) { return "{}"; } }
    private ParseFailureReason mapFailureReason(String message) { if (message==null) return ParseFailureReason.EMPTY_FAULT; for (ParseFailureReason value:ParseFailureReason.values()) if (message.contains(value.message())) return value; return ParseFailureReason.EMPTY_FAULT; }
    private Double round2(double value) { return Math.round(value*100.0)/100.0; }
}

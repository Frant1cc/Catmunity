package org.catmunity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.catmunity.Enum.AIRspEnum;
import org.catmunity.Enum.SourceEnum;
import org.catmunity.advisor.DatabaseChatMemory;
import org.catmunity.constants.AIConstant;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.GlobalException;
import org.catmunity.model.entity.Log;
import org.catmunity.model.vo.*;
import org.catmunity.result.Result;
import org.catmunity.service.ChatService;
import org.catmunity.service.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.model.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final LogService logService;
    private final DatabaseChatMemory databaseChatMemory;

    private final ConcurrentHashMap<String, Sinks.One<Void>> stopSignals = new ConcurrentHashMap<>();

    private static final String TEXT_TAG = "【TEXT】";
    private static final String RESULT_START_TAG = "【RESULT】";
    private static final String RESULT_END_TAG = "【/RESULT】";

    /*public Flux<AiMessage> analyse(String sessionId,
                                   String messageId,
                                   String content,
                                   List<MultipartFile> files) {
        //检查消息ID是否已存在
        if (StringUtils.isBlank(messageId)) {
            return Flux.just(new AiMessage("error", AIConstant.MESSAGE_ID_NOT_NULL));
        }
        if (StringUtils.isBlank(sessionId)) {
            return Flux.just(new AiMessage("error", AIConstant.CONVERSATION_ID_NOT_NULL));
        }
        Long logCount = logService.count(new LambdaQueryWrapper<Log>().eq(Log::getMessageId, messageId)
                .eq(Log::getUserId, LoginContext.getUserId())
                .eq(Log::getSessionId, sessionId));
        if (logCount > 0) {
            return Flux.just(new AiMessage("error", AIConstant.MESSAGE_ID_EXIST));
        }


        Long userId = LoginContext.getUserId();
        List<Media> mediaList = buildMediaList(files);

        Sinks.One<Void> stopSignal = Sinks.one();
        String stopKey = sessionId + ":" + messageId;
        stopSignals.put(stopKey, stopSignal);

        StringBuilder rawResponse = new StringBuilder();
        ParseState state = new ParseState();
        StringBuilder pendingBuffer = new StringBuilder();
        java.util.concurrent.atomic.AtomicBoolean resultSentFlag = new java.util.concurrent.atomic.AtomicBoolean(false);

        Flux<String> chunkFlux = chatClient
                .prompt()
                .user(p -> {
                    if (!mediaList.isEmpty()) {
                        p.text(content).media(mediaList.toArray(new Media[0]));
                    } else {
                        p.text(content);
                    }
                })
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param("userId", userId != null ? userId : 0L)
                        .param("messageId", messageId != null ? messageId : ""))
                .stream()
                .chatResponse()
                .filter(r -> r.getResult().getOutput().getText() != null)
                .map(r -> r.getResult().getOutput().getText())
                .doOnNext(text -> rawResponse.append(text))
                .filter(t -> !t.isEmpty());
        log.debug("【AI对话原始响应】sessionId={}, messageId={}, content={}", sessionId, messageId, rawResponse.toString());

        return chunkFlux
                .window(WINDOW_SIZE)
                .flatMap(window -> window.collectList().map(list -> String.join("", list)))
                .filter(combined -> !combined.isEmpty())
                .flatMap(combined -> processBatch(combined, pendingBuffer, state, sessionId, resultSentFlag))
                .concatWith(Flux.defer(() -> sendFinalResult(sessionId, messageId, rawResponse.toString(), resultSentFlag)))
                .takeUntilOther(stopSignal.asMono())
                .doFinally(signalType -> {
                    stopSignals.remove(stopKey);
                    databaseChatMemory.removeStoppedMark(sessionId, messageId);
                    log.info("【AI对话结束】sessionId={}, messageId={}, signal={}", sessionId, messageId, signalType);
                })
                .onErrorResume(e -> {
                    log.error("【AI对话异常】sessionId={}", sessionId, e);
                    return Flux.just(new AiMessage("error", "AI服务暂时不可用，原因：" + e.getMessage()));
                });
    }*/


    @Override
    public Flux<String> chat(String sessionId, String messageId, String content, List<MultipartFile> files) {
        //检查消息ID是否已存在
        if (StringUtils.isBlank(messageId)) {
            return Flux.just(AIConstant.MESSAGE_ID_NOT_NULL);
        }
        if (StringUtils.isBlank(sessionId)) {
            return Flux.just(AIConstant.CONVERSATION_ID_NOT_NULL);
        }
        Long logCount = logService.count(new LambdaQueryWrapper<Log>().eq(Log::getMessageId, messageId)
                .eq(Log::getUserId, LoginContext.getUserId())
                .eq(Log::getSessionId, sessionId));
        if (logCount > 0) {
            return Flux.just(AIConstant.MESSAGE_ID_EXIST);
        }


        Long userId = LoginContext.getUserId();
        List<Media> mediaList = buildMediaList(files);

        StringBuilder rawResponse = new StringBuilder();
        Flux<String> chunkFlux = chatClient
                .prompt()
                .user(p -> {
                    if (!mediaList.isEmpty()) {
                        p.text(content).media(mediaList.toArray(new Media[0]));
                    } else {
                        p.text(content);
                    }
                })
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param("userId", userId != null ? userId : 0L)
                        .param("messageId", messageId != null ? messageId : ""))
                .stream()
                .content();
        return chunkFlux;
    }

    @Override
    public void clearHistory(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new GlobalException(Result.error(AIConstant.CONVERSATION_ID_NOT_NULL));
        }
        logService.remove(new LambdaQueryWrapper<Log>().eq(Log::getSessionId, sessionId));
    }

    @Override
    public List<SessionInfo> getSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new GlobalException(Result.error(AIConstant.CONVERSATION_ID_NOT_NULL));
        }

        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Log::getSessionId, sessionId).orderByAsc(Log::getCreatedAt);
        List<Log> logs = logService.list(wrapper);

        List<SessionInfo> sessionInfos = new ArrayList<>();
        for (Log log : logs) {
            if (log.getText().contains(TEXT_TAG) || log.getText().contains(RESULT_START_TAG)) {
                String content = extractPureText(log.getText());
                AIChatResponse result = extractStructuredResult(log.getText());
                Object obj = null;
                if (result != null ) {
                    result.setSessionId(log.getSessionId());
                    result.setMessageId(log.getMessageId());
                    result.setTimestamp(log.getCreatedAt().getTime());
                    result.setAnswer(content);
                    if (result.getSourcesStr() != null) {
                        List<Source> sources = extraceSources(sessionId, result);
                        result.setSources(sources);
                    }
                    switch (AIRspEnum.fromCode(result.getResponseType())) {
                        case RECOMMENDATION -> obj = BeanUtil.copyProperties(result, AIRecommendationVO.class);
                        case SEARCHRESULT ->   obj = BeanUtil.copyProperties(result, AISearchResultVO.class);
                        case IMAGE_SEARCH -> obj = BeanUtil.copyProperties(result, AIImageSearchVO.class);
                        default -> obj = result;
                    }
                }

                try {
                    sessionInfos.add(SessionInfo.builder()
                            .sessionId(sessionId)
                            .messageId(log.getMessageId())
                            .content(content)
                            .result(obj)
                            .messageType(log.getMessageType().name())
                            .createdAt(log.getCreatedAt()).build());
                } catch (Exception e) {
                    throw new GlobalException(Result.error(AIConstant.JSON_PARSE_FAILED));
                }
            }else {
                sessionInfos.add(SessionInfo.builder()
                        .sessionId(sessionId)
                        .messageId(log.getMessageId())
                        .content(log.getText())
                        .messageType(log.getMessageType().name())
                        .createdAt(log.getCreatedAt()).build());
            }
        }
        return sessionInfos;
    }

    @Override
    public List<SessionList> getSessions() {
        Long userId = LoginContext.getUserId();

        List<Log> logs = logService.list(new LambdaQueryWrapper<Log>().eq(Log::getUserId, userId).orderByAsc(Log::getCreatedAt));

        List<SessionList> sessionLists = new ArrayList<>();
        Map<String, SessionList> sessionMap = new HashMap<>();

        for (Log log : logs) {
            if (StringUtils.isEmpty(log.getSessionId()) || log.getMessageType() != MessageType.USER
            || sessionMap.containsKey(log.getSessionId())) {
                continue;
            }
            sessionMap.put(log.getSessionId(), SessionList.builder().sessionId(log.getSessionId()).title(log.getText()).createdAt(log.getCreatedAt()).build());
            sessionLists.add(SessionList.builder()
                    .sessionId(log.getSessionId())
                    .title(log.getText())
                    .createdAt(log.getCreatedAt()).build());
        }

        return sessionLists;
    }

    @Override
    public void stopChat(String sessionId, String messageId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new GlobalException(Result.error(AIConstant.CONVERSATION_ID_NOT_NULL));
        }
        if (StringUtils.isEmpty(messageId)) {
            throw new GlobalException(Result.error(AIConstant.MESSAGE_ID_NOT_NULL));
        }
        String stopKey = sessionId + ":" + messageId;
        Sinks.One<Void> stopSignal = stopSignals.get(stopKey);
        if (stopSignal == null) {
            log.warn("【停止对话】未找到活跃的对话流，sessionId={}, messageId={}", sessionId, messageId);
            return;
        }
        Sinks.EmitResult result = stopSignal.tryEmitEmpty();
        if (result.isFailure()) {
            log.warn("【停止对话失败】sessionId={}, messageId={}, result={}", sessionId, messageId, result);
            throw new GlobalException(Result.error(AIConstant.STOP_CONVERSATION_FAILED));
        }
        databaseChatMemory.markAsStopped(sessionId, messageId);
        stopSignals.remove(stopKey);
        log.info("【停止对话成功】sessionId={}, messageId={}", sessionId, messageId);
    }

    @NotNull
    private List<Source> extraceSources(String sessionId, AIChatResponse result) {
        List<String> aiSources = result.getSourcesStr() != null ? result.getSourcesStr() : new ArrayList<>();

        List<Source> sources = new ArrayList<>();
        for (String source : aiSources) {
            int startIdx = source.indexOf("【来源：");
            int endIdx = source.indexOf("】");
            if (startIdx >= 0 && endIdx > startIdx) {
                String title = source.substring(startIdx + 4, endIdx);
                String content = source.substring(endIdx + 1);
                sources.add(Source.builder().title(title).sourceType(SourceEnum.DATABASE.getSourceType()).content(content).build());
            } else {
                sources.add(Source.builder().title("未知来源").sourceType(SourceEnum.DATABASE.getSourceType()).content(source).build());
            }
        }
        return sources;
    }

    private AIChatResponse extractStructuredResult(String raw) {
        int start = raw.indexOf(RESULT_START_TAG);
        int end = raw.indexOf(RESULT_END_TAG);
        if (start < 0) return null;

        String json = start >= 0 ? raw.substring(start + RESULT_START_TAG.length(),
                end > start ? end : raw.length()) : "";
        json = cleanJsonContent(json);

        try {
            return objectMapper.readValue(json, AIChatResponse.class);
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String cleanJsonContent(String json) {
        if (json == null || json.isEmpty()) return json;
        String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "")
                .replaceAll("```", "").replaceAll("\\s*【/RESULT】\\s*", "").trim();
        int s = cleaned.indexOf("{");
        int e = cleaned.lastIndexOf("}");
        return s >= 0 && e > s ? cleaned.substring(s, e + 1) : cleaned.trim();
    }

    private String extractPureText(String full) {
        if (full == null || full.isEmpty()) return "";
        String text = full;
        int textStart = text.indexOf(TEXT_TAG);
        if (textStart >= 0) {
            int resultStart = text.indexOf(RESULT_START_TAG);
            text = resultStart > textStart ?
                    text.substring(textStart + TEXT_TAG.length(), resultStart) :
                    text.substring(textStart + TEXT_TAG.length());
        }
        return text.replaceAll("```json\\s*", "").replaceAll("```", "")
                .replaceAll(RESULT_START_TAG, "").replaceAll(RESULT_END_TAG, "").trim();
    }

    private List<Media> buildMediaList(List<MultipartFile> files) {
        List<Media> list = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    try {
                        if (f.getContentType() != null) {
                            list.add(new Media(MimeType.valueOf(f.getContentType()), f.getResource()));
                        }
                    } catch (Exception e) {
                        log.warn("文件格式解析失败: {}", e.getMessage());
                    }
                }
            }
        }
        return list;
    }
}
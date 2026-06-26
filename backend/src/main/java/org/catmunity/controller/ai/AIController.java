package org.catmunity.controller.ai;

import org.catmunity.aop.AccessLimit;
import org.catmunity.model.vo.*;
import org.catmunity.result.Result;
import org.catmunity.service.ChatService;
import org.catmunity.utils.StructuredOutputConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@Tag(name = "用户端-智慧助手")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private StructuredOutputConverter structuredOutputConverter;

    @Autowired
    private ChatService chatService;

    @AccessLimit
    @Operation(summary = "RAG增强对话", description = "基于向量知识库和猫咪知识库的智能对话服务")
    @PostMapping(value = "/chat/messages", produces = "text/html;charset=utf-8")
    public Flux<String> chat(
             @RequestParam(required = true) String sessionId,
             @RequestParam(required = true) String messageId,
             @RequestParam(required = false) String content,
             List<MultipartFile> files) {

        return chatService.chat(sessionId, messageId, content, files);
    }

    @Operation(summary = "清除会话历史")
    @DeleteMapping("/chat/sessions/{sessionId}")
    public Result<String> clearHistory(@PathVariable String sessionId) {
        chatService.clearHistory(sessionId);
        return Result.success("对话历史已清除");
    }
    
    @Operation(summary = "获取会话列表")
    @GetMapping("/chat/sessions")
    public Result<List<SessionList>> getSessions() {
        return Result.success(chatService.getSessions());
    }
    
    
    @Operation(summary = "获取会话详情")
    @GetMapping("/chat/sessions/{sessionId}")
    public Result<List<SessionInfo>> getSession(@PathVariable String sessionId) {
        List<SessionInfo> sessionInfos = chatService.getSession(sessionId);
        return Result.success(sessionInfos);
    }

    @Operation(summary = "停止对话")
    @PostMapping("/chat/messages/{sessionId}/{messageId}/stop")
    public Result<String> stopChat(@PathVariable String sessionId, @PathVariable String messageId) {
        chatService.stopChat(sessionId, messageId);
        return Result.success("对话已停止");
    }
}
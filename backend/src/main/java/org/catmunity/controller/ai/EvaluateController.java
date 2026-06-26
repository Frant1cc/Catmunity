package org.catmunity.controller.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.catmunity.model.dto.EvaluateDTO;
import org.catmunity.model.dto.EvaluatePageDTO;
import org.catmunity.model.entity.Evaluate;
import org.catmunity.result.Result;
import org.catmunity.service.EvaluateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-ai对话评价管理")
@RestController
@Slf4j
@RequestMapping("/ai/evaluate")
@RequiredArgsConstructor
public class EvaluateController {
    
    private final EvaluateService evaluateService;
    
    @Operation(summary = "添加评价")
    @PostMapping("/add")
    public Result<Evaluate> addEvaluate(@RequestBody EvaluateDTO evaluateDTO) {
        return Result.success(evaluateService.addEvaluate(evaluateDTO));
    }
    
    @Operation(summary = "根据消息ID获取评价")
    @GetMapping("/message/{messageId}")
    public Result<Evaluate> getEvaluateByMessageId(
            @Parameter(description = "消息ID") @PathVariable String messageId) {
        return Result.success(evaluateService.getEvaluateByMessageId(messageId));
    }
    
    @Operation(summary = "根据会话ID获取最新评价")
    @GetMapping("/session/{sessionId}")
    public Result<Evaluate> getEvaluateBySessionId(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        return Result.success(evaluateService.getEvaluateBySessionId(sessionId));
    }

    @Operation(summary = "分页查询评价")
    @GetMapping("/page")
    public Result<Page<Evaluate>> getEvaluatePage(EvaluatePageDTO evaluatePageDTO) {
        return Result.success(evaluateService.getEvaluatePage(evaluatePageDTO));
    }
    
    @Operation(summary = "更新评价")
    @PutMapping("/update")
    public Result<Evaluate> updateEvaluate(@RequestBody EvaluateDTO evaluateDTO) {
        return Result.success(evaluateService.updateEvaluate(evaluateDTO));
    }
    
    @Operation(summary = "删除评价")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteEvaluate(
            @Parameter(description = "评价ID") @PathVariable Long id) {
        return Result.success(evaluateService.deleteEvaluate(id));
    }
    
    @Operation(summary = "根据消息ID删除评价")
    @DeleteMapping("/delete/message/{messageId}")
    public Result<Boolean> deleteEvaluateByMessageId(
            @Parameter(description = "消息ID") @PathVariable String messageId) {
        return Result.success(evaluateService.deleteEvaluateByMessageId(messageId));
    }
}
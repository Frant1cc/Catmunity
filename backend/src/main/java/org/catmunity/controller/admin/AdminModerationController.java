package org.catmunity.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.dto.ModerationActionDTO;
import org.catmunity.model.dto.ModerationQueryDTO;
import org.catmunity.model.vo.ModerationRecordVO;
import org.catmunity.model.vo.ReportVO;
import org.catmunity.result.PageResult;
import org.catmunity.result.Result;
import org.catmunity.service.ModerationService;
import org.catmunity.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "管理端-内容审核")
@RestController
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
@Slf4j
public class AdminModerationController {

    private final ModerationService moderationService;
    private final ReportService reportService;

    @Operation(summary = "待审核内容列表")
    @GetMapping("/pending")
    public Result<PageResult<ModerationRecordVO>> getPendingList(
            @RequestParam(required = false) Integer contentType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        ModerationQueryDTO queryDTO = ModerationQueryDTO.builder()
                .contentType(contentType)
                .status(status)
                .riskLevel(riskLevel)
                .startTime(startTime)
                .endTime(endTime)
                .keyword(keyword)
                .build();

        PageResult<ModerationRecordVO> result = moderationService.getPendingList(queryDTO, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "审核记录日志")
    @GetMapping("/logs")
    public Result<PageResult<ModerationRecordVO>> getModerationLogs(
            @RequestParam(required = false) Integer contentType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        ModerationQueryDTO queryDTO = ModerationQueryDTO.builder()
                .contentType(contentType)
                .status(status)
                .riskLevel(riskLevel)
                .startTime(startTime)
                .endTime(endTime)
                .keyword(keyword)
                .build();

        PageResult<ModerationRecordVO> result = moderationService.getModerationLogs(queryDTO, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "审核详情")
    @GetMapping("/detail/{id}")
    public Result<ModerationRecordVO> getModerationDetail(@PathVariable Long id) {
        ModerationRecordVO vo = moderationService.getModerationDetail(id);
        return Result.success(vo);
    }

    @Operation(summary = "通过内容")
    @PostMapping("/approve")
    public Result<?> approveContent(
            @RequestParam Long moderationId,
            @RequestParam(required = false) String comment) {
        moderationService.approveContent(moderationId, comment);
        return Result.success("内容已通过");
    }

    @Operation(summary = "拒绝内容")
    @PostMapping("/reject")
    public Result<?> rejectContent(
            @RequestParam Long moderationId,
            @RequestParam String reason) {
        moderationService.rejectContent(moderationId, reason);
        return Result.success("内容已拒绝");
    }

    @Operation(summary = "批量通过")
    @PostMapping("/approve/batch")
    public Result<?> batchApprove(@RequestBody List<Long> moderationIds) {
        moderationService.batchApprove(moderationIds);
        return Result.success("批量通过成功");
    }

    @Operation(summary = "批量拒绝")
    @PostMapping("/reject/batch")
    public Result<?> batchReject(
            @RequestBody List<Long> moderationIds,
            @RequestParam String reason) {
        moderationService.batchReject(moderationIds, reason);
        return Result.success("批量拒绝成功");
    }

    @Operation(summary = "待处理举报列表")
    @GetMapping("/reports/pending")
    public Result<PageResult<ReportVO>> getPendingReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ReportVO> result = reportService.getPendingReports(pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "所有举报列表")
    @GetMapping("/reports")
    public Result<PageResult<ReportVO>> getAllReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ReportVO> result = reportService.getAllReports(pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "处理举报")
    @PostMapping("/reports/handle")
    public Result<?> handleReport(
            @RequestParam Long reportId,
            @RequestParam Integer action,
            @RequestParam(required = false) String result) {
        Long handlerId = org.catmunity.context.LoginContext.getUserId();
        reportService.handleReport(reportId, handlerId, action, result);
        return Result.success("处理成功");
    }

    @Operation(summary = "批量处理举报")
    @PostMapping("/reports/handle/batch")
    public Result<?> batchHandleReports(
            @RequestBody List<Long> reportIds,
            @RequestParam Integer action,
            @RequestParam(required = false) String result) {
        Long handlerId = org.catmunity.context.LoginContext.getUserId();
        reportService.batchHandleReports(reportIds, handlerId, action, result);
        return Result.success("批量处理成功");
    }
}
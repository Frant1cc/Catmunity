package org.catmunity.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.vo.*;
import org.catmunity.result.Result;
import org.catmunity.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-数据看板")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取概览数据", description = "获取仪表盘核心指标数据：总用户数、今日新增、活跃用户、帖子总数等")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> getOverview() {
        DashboardOverviewVO overview = dashboardService.getOverview();
        return Result.success(overview);
    }

    @Operation(summary = "获取用户增长趋势", description = "获取用户增长趋势折线图数据，支持日/周/月维度")
    @GetMapping("/user-growth")
    public Result<UserGrowthTrendVO> getUserGrowthTrend(
            @RequestParam(defaultValue = "day") String dimension,
            @RequestParam(defaultValue = "30") Integer days) {
        UserGrowthTrendVO trend = dashboardService.getUserGrowthTrend(dimension, days);
        return Result.success(trend);
    }

    @Operation(summary = "获取内容发布统计", description = "获取内容发布量统计数据（帖子、打卡、评论）")
    @GetMapping("/content-stats")
    public Result<ContentStatsVO> getContentStats() {
        ContentStatsVO stats = dashboardService.getContentStats();
        return Result.success(stats);
    }

    @Operation(summary = "获取AI功能使用量", description = "获取AI功能使用量统计数据（问答次数、识别次数、摘要生成次数）")
    @GetMapping("/ai-usage")
    public Result<AIFeatureUsageVO> getAIFeatureUsage() {
        AIFeatureUsageVO usage = dashboardService.getAIFeatureUsage();
        return Result.success(usage);
    }

    @Operation(summary = "获取内容审核统计", description = "获取内容审核通过率统计数据")
    @GetMapping("/moderation-stats")
    public Result<ModerationStatsVO> getModerationStats() {
        ModerationStatsVO stats = dashboardService.getModerationStats();
        return Result.success(stats);
    }

    @Operation(summary = "获取猫咪品种分布", description = "获取猫咪品种分布饼图数据")
    @GetMapping("/cat-breed-distribution")
    public Result<CatBreedDistributionVO> getCatBreedDistribution() {
        CatBreedDistributionVO distribution = dashboardService.getCatBreedDistribution();
        return Result.success(distribution);
    }
}
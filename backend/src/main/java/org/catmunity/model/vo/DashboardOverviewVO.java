package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Schema(description = "仪表盘概览数据")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO implements Serializable {

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "今日新增用户数")
    private Long todayNewUsers;

    @Schema(description = "活跃用户数（7天内有活动的用户）")
    private Long activeUsers;

    @Schema(description = "帖子总数")
    private Long totalPosts;

    @Schema(description = "今日新增帖子数")
    private Long todayNewPosts;

    @Schema(description = "猫咪档案总数")
    private Long totalCatProfiles;

    @Schema(description = "打卡总数")
    private Long totalCheckins;

    @Schema(description = "今日新增打卡数")
    private Long todayNewCheckins;

    @Schema(description = "待审核内容数")
    private Long pendingModerationCount;
}
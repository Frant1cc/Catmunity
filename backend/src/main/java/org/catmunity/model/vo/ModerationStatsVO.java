package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Schema(description = "内容审核统计")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationStatsVO implements Serializable {

    @Schema(description = "待审核数量")
    private Long pendingCount;

    @Schema(description = "已通过数量")
    private Long approvedCount;

    @Schema(description = "已拒绝数量")
    private Long rejectedCount;

    @Schema(description = "审核通过率（百分比）")
    private Double passRate;

    @Schema(description = "AI标记待复核数量")
    private Long autoFlaggedCount;

    @Schema(description = "今日审核数量")
    private Long todayModeratedCount;

    @Schema(description = "今日通过数量")
    private Long todayApprovedCount;

    @Schema(description = "今日拒绝数量")
    private Long todayRejectedCount;
}
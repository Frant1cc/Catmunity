package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Schema(description = "内容发布统计")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentStatsVO implements Serializable {

    @Schema(description = "帖子总数")
    private Long totalPosts;

    @Schema(description = "今日新增帖子数")
    private Long todayNewPosts;

    @Schema(description = "打卡总数")
    private Long totalCheckins;

    @Schema(description = "今日新增打卡数")
    private Long todayNewCheckins;

    @Schema(description = "评论总数")
    private Long totalComments;

    @Schema(description = "今日新增评论数")
    private Long todayNewComments;

    @Schema(description = "内容发布趋势数据")
    private List<TrendDataVO> trendData;
}
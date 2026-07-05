package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "审核记录视图对象")
public class ModerationRecordVO implements Serializable {

    @Schema(description = "审核记录ID")
    private Long id;

    @Schema(description = "内容类型：1-帖子，2-评论")
    private Integer contentType;

    @Schema(description = "内容类型描述")
    private String contentTypeStr;

    @Schema(description = "内容ID")
    private Long contentId;

    @Schema(description = "内容摘要")
    private String contentSummary;

    @Schema(description = "原始内容")
    private String originalContent;

    @Schema(description = "发布者ID")
    private Long authorId;

    @Schema(description = "发布者名称")
    private String authorName;

    @Schema(description = "AI风险评分")
    private Double aiRiskScore;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "风险等级描述")
    private String riskLevelStr;

    @Schema(description = "风险等级颜色")
    private String riskLevelColor;

    @Schema(description = "违规类型列表")
    private List<String> violationTypes;

    @Schema(description = "AI详细分析")
    private String aiAnalysis;

    @Schema(description = "审核状态")
    private Integer status;

    @Schema(description = "审核状态描述")
    private String statusStr;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人名称")
    private String reviewerName;

    @Schema(description = "审核意见")
    private String reviewComment;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
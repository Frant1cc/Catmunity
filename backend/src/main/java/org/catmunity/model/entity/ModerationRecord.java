package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "moderation_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "内容审核记录实体")
public class ModerationRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "审核记录ID")
    private Long id;

    @Schema(description = "内容类型：1-帖子，2-评论")
    private Integer contentType;

    @Schema(description = "内容ID（帖子ID或评论ID）")
    private Long contentId;

    @Schema(description = "内容摘要")
    private String contentSummary;

    @Schema(description = "原始内容")
    private String originalContent;

    @Schema(description = "发布者ID")
    private Long authorId;

    @Schema(description = "AI风险评分（0-1）")
    private Double aiRiskScore;

    @Schema(description = "风险等级：0-安全，1-低风险，2-中风险，3-高风险，4-极高风险")
    private Integer riskLevel;

    @Schema(description = "AI检测到的违规类型（多个用逗号分隔）")
    private String violationTypes;

    @Schema(description = "AI详细分析结果")
    private String aiAnalysis;

    @Schema(description = "审核状态：0-待审核，1-已通过，2-已拒绝，3-AI标记待复核")
    private Integer status;

    @Schema(description = "审核人ID（管理员）")
    private Long reviewerId;

    @Schema(description = "审核人名称")
    private String reviewerName;

    @Schema(description = "审核意见/拒绝原因")
    private String reviewComment;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
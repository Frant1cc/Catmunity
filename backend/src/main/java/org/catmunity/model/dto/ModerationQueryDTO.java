package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "内容审核查询条件")
public class ModerationQueryDTO {

    @Schema(description = "内容类型：1-帖子，2-评论")
    private Integer contentType;

    @Schema(description = "审核状态：0-待审核，1-已通过，2-已拒绝，3-AI标记待复核")
    private Integer status;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "关键字搜索")
    private String keyword;
}
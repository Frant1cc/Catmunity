package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "审核操作DTO")
public class ModerationActionDTO {

    @Schema(description = "审核记录ID")
    private Long moderationId;

    @Schema(description = "操作类型：1-通过，2-拒绝")
    private Integer action;

    @Schema(description = "审核意见/拒绝原因")
    private String comment;
}
package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "举报记录视图对象")
public class ReportVO implements Serializable {

    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "内容类型")
    private Integer contentType;

    @Schema(description = "内容类型描述")
    private String contentTypeStr;

    @Schema(description = "被举报内容ID")
    private Long contentId;

    @Schema(description = "被举报内容摘要")
    private String contentSummary;

    @Schema(description = "被举报人ID")
    private Long reportedUserId;

    @Schema(description = "被举报人名称")
    private String reportedUserName;

    @Schema(description = "举报人ID")
    private Long reporterId;

    @Schema(description = "举报人名称")
    private String reporterName;

    @Schema(description = "举报原因类型")
    private Integer reasonType;

    @Schema(description = "举报原因描述")
    private String reasonDescription;

    @Schema(description = "举报状态")
    private Integer status;

    @Schema(description = "举报状态描述")
    private String statusStr;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理人名称")
    private String handlerName;

    @Schema(description = "处理结果")
    private String handlerResult;

    @Schema(description = "处理时间")
    private LocalDateTime handledAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
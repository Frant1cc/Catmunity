package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户举报实体")
public class Report implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "举报内容类型：1-帖子，2-评论")
    private Integer contentType;

    @Schema(description = "被举报内容ID")
    private Long contentId;

    @Schema(description = "被举报人ID")
    private Long reportedUserId;

    @Schema(description = "举报人ID")
    private Long reporterId;

    @Schema(description = "举报原因类型")
    private Integer reasonType;

    @Schema(description = "举报原因描述")
    private String reasonDescription;

    @Schema(description = "举报状态：0-待处理，1-已处理，2-已忽略")
    private Integer status;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理结果")
    private String handlerResult;

    @Schema(description = "处理时间")
    private LocalDateTime handledAt;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
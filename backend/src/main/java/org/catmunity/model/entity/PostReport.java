package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "post_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子举报实体")
public class PostReport implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "举报用户ID")
    private Long reporterId;

    @Schema(description = "被举报用户ID")
    private Long reportedUserId;

    @Schema(description = "举报类型：1-垃圾广告，2-色情低俗，3-暴力血腥，4-违法犯罪，5-虚假信息，6-人身攻击，99-其他")
    private Integer reportType;

    @Schema(description = "举报描述")
    private String description;

    @Schema(description = "状态：0-待处理，1-已处理，2-已驳回")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "处理时间")
    private LocalDateTime handledAt;
}
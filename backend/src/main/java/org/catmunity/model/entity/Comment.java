package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "comment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论实体")
public class Comment implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "父评论ID（用于回复）")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "状态：0-正常，1-违规，2-已删除")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
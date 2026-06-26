package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "post_like")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子点赞实体")
public class PostLike implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "点赞ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "用户ID")
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
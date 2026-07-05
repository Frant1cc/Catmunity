package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "follow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关注关系实体")
public class Follow implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关注者ID")
    private Long followerId;

    @Schema(description = "被关注者ID")
    private Long followingId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
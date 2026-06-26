package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论数据传输对象")
public class CommentDTO {

    @Schema(description = "评论ID（更新时必填）")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "父评论ID（用于回复）")
    private Long parentId;

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不能为空")
    private String content;
}
package org.catmunity.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "评论视图对象")
public class CommentVO implements Serializable {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "子评论列表")
    private List<CommentVO> children;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
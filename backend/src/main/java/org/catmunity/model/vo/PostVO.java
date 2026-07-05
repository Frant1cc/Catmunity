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
@Schema(description = "帖子视图对象")
public class PostVO implements Serializable {

    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "图片URL列表")
    private List<String> images;

    @Schema(description = "话题标签列表")
    private List<String> tags;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "是否已点赞")
    private Boolean isLiked;

    @Schema(description = "是否已收藏")
    private Boolean isFavorited;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
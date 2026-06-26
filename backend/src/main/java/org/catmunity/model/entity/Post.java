package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@TableName(value = "post")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子实体")
public class Post implements Serializable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "关联猫咪档案ID")
    private Long catProfileId;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "图片URL列表（JSON格式存储）")
    @TableField("images")
    private String imagesJson;

    @Schema(description = "标签列表（JSON格式存储）")
    @TableField("tags")
    private String tagsJson;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "转发数")
    private Integer repostCount;

    @Schema(description = "状态：0-正常，1-违规待处理，2-已删除")
    private Integer status;

    @Schema(description = "违规原因")
    private String violationReason;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public List<String> getImages() {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(imagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            this.imagesJson = "[]";
        } else {
            try {
                this.imagesJson = objectMapper.writeValueAsString(images);
            } catch (JsonProcessingException e) {
                this.imagesJson = "[]";
            }
        }
    }

    public List<String> getTags() {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tagsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            this.tagsJson = "[]";
        } else {
            try {
                this.tagsJson = objectMapper.writeValueAsString(tags);
            } catch (JsonProcessingException e) {
                this.tagsJson = "[]";
            }
        }
    }
}
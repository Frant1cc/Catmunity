package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子数据传输对象")
public class PostDTO {

    @Schema(description = "帖子ID（更新时必填）")
    private Long id;

    @Schema(description = "帖子内容")
    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 2000, message = "帖子内容不能超过2000字")
    private String content;

    @Schema(description = "图片URL列表（最多9张）")
    @Size(max = 9, message = "图片最多9张")
    private List<String> images;

    @Schema(description = "话题标签列表")
    private List<String> tags;
}
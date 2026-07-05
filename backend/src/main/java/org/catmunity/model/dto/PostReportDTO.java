package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "举报数据传输对象")
public class PostReportDTO {

    @Schema(description = "帖子ID")
    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    @Schema(description = "被举报用户ID")
    @NotNull(message = "被举报用户ID不能为空")
    private Long reportedUserId;

    @Schema(description = "举报类型：1-垃圾广告，2-色情低俗，3-暴力血腥，4-违法犯罪，5-虚假信息，6-人身攻击，99-其他")
    @NotNull(message = "举报类型不能为空")
    private Integer reportType;

    @Schema(description = "举报描述")
    private String description;
}
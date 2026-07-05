package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猫咪打卡数据传输对象")
public class CatCheckinDTO {

    @Schema(description = "打卡ID（更新时必填）")
    private Long id;

    @Schema(description = "猫咪档案ID")
    private Long catProfileId;

    @Schema(description = "打卡照片URL")
    private String photoUrl;

    @Schema(description = "打卡备注")
    private String remark;

    @Schema(description = "是否同步到社区：true-同步，false-不同步")
    private Boolean syncToPost;
}
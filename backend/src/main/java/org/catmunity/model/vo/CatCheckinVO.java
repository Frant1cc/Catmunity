package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猫咪打卡视图对象")
public class CatCheckinVO implements Serializable {

    @Schema(description = "打卡ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "猫咪档案ID")
    private Long catProfileId;

    @Schema(description = "猫咪名字")
    private String catName;

    @Schema(description = "打卡日期")
    private LocalDate checkinDate;

    @Schema(description = "打卡照片URL")
    private String photoUrl;

    @Schema(description = "打卡备注")
    private String remark;

    @Schema(description = "是否已同步到社区")
    private Boolean syncedToPost;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
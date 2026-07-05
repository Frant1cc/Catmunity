package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName(value = "cat_checkin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猫咪打卡实体")
public class CatCheckin implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "打卡ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "猫咪档案ID")
    private Long catProfileId;

    @Schema(description = "打卡日期")
    private LocalDate checkinDate;

    @Schema(description = "打卡照片URL")
    private String photoUrl;

    @Schema(description = "打卡备注")
    private String remark;

    @Schema(description = "是否已同步到社区：0-否，1-是")
    private Integer syncedToPost;

    @Schema(description = "关联的帖子ID（同步后填充）")
    private Long postId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
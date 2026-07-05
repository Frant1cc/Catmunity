package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端猫咪档案查询条件")
public class CatProfileQueryDTO {

    @Schema(description = "猫咪名称（模糊搜索）")
    private String name;

    @Schema(description = "品种（模糊搜索）")
    private String breed;

    @Schema(description = "性别：0-母猫，1-公猫")
    private Integer gender;

    @Schema(description = "创建时间开始")
    private LocalDateTime createdAtStart;

    @Schema(description = "创建时间结束")
    private LocalDateTime createdAtEnd;

    @Schema(description = "档案状态：0-正常，1-已领养，2-其他")
    private Integer status;
}
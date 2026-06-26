package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "猫咪档案视图对象")
public class CatProfileVO implements Serializable {

    @Schema(description = "猫咪档案ID")
    private Long id;

    @Schema(description = "猫咪名字")
    private String name;

    @Schema(description = "猫咪品种")
    private String breed;

    @Schema(description = "猫咪性别：0-母猫，1-公猫")
    private Integer gender;

    @Schema(description = "猫咪生日")
    private LocalDate birthday;

    @Schema(description = "猫咪体重（kg）")
    private BigDecimal weight;

    @Schema(description = "猫咪照片URL列表")
    private List<String> photoUrls;

    @Schema(description = "猫咪状态：0-正常，1-已领养，2-其他")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "健康事件列表（时间线展示用）")
    private List<CatHealthEventVO> healthEvents;
}
package org.catmunity.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猫咪档案Excel导出VO")
public class CatProfileExcelVO {

    @ExcelProperty("猫咪ID")
    @Schema(description = "猫咪档案ID")
    private Long id;

    @ExcelProperty("猫咪名字")
    @Schema(description = "猫咪名字")
    private String name;

    @ExcelProperty("品种")
    @Schema(description = "猫咪品种")
    private String breed;

    @ExcelProperty("性别")
    @Schema(description = "猫咪性别")
    private String genderStr;

    @ExcelProperty("生日")
    @Schema(description = "猫咪生日")
    private LocalDate birthday;

    @ExcelProperty("体重(kg)")
    @Schema(description = "猫咪体重")
    private BigDecimal weight;

    @ExcelProperty("状态")
    @Schema(description = "猫咪状态")
    private String statusStr;

    @ExcelProperty("创建时间")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @ExcelProperty("更新时间")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
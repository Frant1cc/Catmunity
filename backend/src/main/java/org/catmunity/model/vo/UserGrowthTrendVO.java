package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Schema(description = "用户增长趋势数据")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGrowthTrendVO implements Serializable {

    @Schema(description = "时间维度：day-日，week-周，month-月")
    private String dimension;

    @Schema(description = "趋势数据点列表")
    private List<TrendDataVO> data;

    @Schema(description = "总增长量")
    private Long totalGrowth;
}
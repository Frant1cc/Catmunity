package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Schema(description = "趋势数据点")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataVO implements Serializable {

    @Schema(description = "日期（yyyy-MM-dd格式）")
    private String date;

    @Schema(description = "数量")
    private Long count;
}
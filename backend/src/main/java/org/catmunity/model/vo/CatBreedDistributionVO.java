package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Schema(description = "猫咪品种分布数据")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatBreedDistributionVO implements Serializable {

    @Schema(description = "品种分布数据列表")
    private List<BreedDataVO> breeds;

    @Schema(description = "总猫咪数量")
    private Long totalCats;

    @Schema(description = "品种数量")
    private Integer breedCount;

    @Schema(description = "未知/其他品种数量")
    private Long unknownBreedCount;

    @Schema(description = "品种数据")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreedDataVO implements Serializable {

        @Schema(description = "品种名称")
        private String breed;

        @Schema(description = "数量")
        private Long count;

        @Schema(description = "占比（百分比）")
        private Double percentage;
    }
}
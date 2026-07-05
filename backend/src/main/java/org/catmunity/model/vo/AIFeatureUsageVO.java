package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Schema(description = "AI功能使用量统计")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIFeatureUsageVO implements Serializable {

    @Schema(description = "问答次数（AI对话）")
    private Long chatCount;

    @Schema(description = "图片识别次数")
    private Long imageRecognitionCount;

    @Schema(description = "摘要生成次数")
    private Long summaryGenerationCount;

    @Schema(description = "AI功能使用趋势数据")
    private List<TrendDataVO> trendData;

    @Schema(description = "今日AI对话次数")
    private Long todayChatCount;

    @Schema(description = "今日图片识别次数")
    private Long todayImageRecognitionCount;

    @Schema(description = "今日摘要生成次数")
    private Long todaySummaryGenerationCount;
}
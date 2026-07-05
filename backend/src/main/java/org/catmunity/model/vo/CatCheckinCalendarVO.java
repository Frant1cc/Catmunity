package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猫咪打卡日历视图对象")
public class CatCheckinCalendarVO implements Serializable {

    @Schema(description = "年")
    private Integer year;

    @Schema(description = "月")
    private Integer month;

    @Schema(description = "打卡日期集合")
    private List<LocalDate> checkinDates;

    @Schema(description = "当月打卡天数")
    private Integer checkinCount;

    @Schema(description = "连续打卡天数")
    private Integer consecutiveDays;

    @Schema(description = "本月是否全勤")
    private Boolean fullMonth;
}
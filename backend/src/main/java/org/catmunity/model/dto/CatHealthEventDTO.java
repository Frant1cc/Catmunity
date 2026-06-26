package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猫咪健康事件数据传输对象")
public class CatHealthEventDTO {

    @Schema(description = "健康事件ID（更新时必填）")
    private Long id;

    @Schema(description = "猫咪档案ID")
    private Long catProfileId;

    @Schema(description = "事件类型：1-疫苗接种，2-驱虫，3-绝育，4-体检，5-疾病治疗，6-美容护理，99-其他")
    private Integer eventType;

    @Schema(description = "事件标题")
    private String title;

    @Schema(description = "事件描述")
    private String description;

    @Schema(description = "事件日期")
    private LocalDate eventDate;

    @Schema(description = "下次提醒日期（可选）")
    private LocalDate nextRemindDate;

    @Schema(description = "备注")
    private String remark;
}
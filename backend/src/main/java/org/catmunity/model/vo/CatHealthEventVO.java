package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "猫咪健康事件视图对象")
public class CatHealthEventVO implements Serializable {

    @Schema(description = "健康事件ID")
    private Long id;

    @Schema(description = "猫咪档案ID")
    private Long catProfileId;

    @Schema(description = "事件类型：1-疫苗接种，2-驱虫，3-绝育，4-体检，5-疾病治疗，6-美容护理，99-其他")
    private Integer eventType;

    @Schema(description = "事件类型描述")
    private String eventTypeDesc;

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

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
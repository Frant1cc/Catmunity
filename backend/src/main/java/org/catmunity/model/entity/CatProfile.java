package org.catmunity.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@TableName(value = "cat_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "猫咪档案实体")
public class CatProfile implements Serializable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @TableId(value = "id", type = IdType.AUTO)
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

    @Schema(description = "猫咪照片URL列表（JSON格式存储）")
    @TableField("photo_urls")
    private String photoUrlsJson;

    @Schema(description = "猫咪状态：0-正常，1-已领养，2-其他")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public List<String> getPhotoUrls() {
        if (photoUrlsJson == null || photoUrlsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(photoUrlsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setPhotoUrls(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            this.photoUrlsJson = "[]";
        } else {
            try {
                this.photoUrlsJson = objectMapper.writeValueAsString(photoUrls);
            } catch (JsonProcessingException e) {
                this.photoUrlsJson = "[]";
            }
        }
    }
}
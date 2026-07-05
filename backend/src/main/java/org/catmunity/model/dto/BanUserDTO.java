package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "封禁/解封用户DTO")
public class BanUserDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "封禁原因")
    private String reason;
}
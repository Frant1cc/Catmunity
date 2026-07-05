package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetFilesPageDTO {
    
    @Schema(description = "当前页码")
    private Integer current;
    
    @Schema(description = "每页数量")
    private Integer size;
}
package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端用户查询条件")
public class UserQueryDTO {

    @Schema(description = "昵称/账号（模糊搜索）")
    private String keyword;

    @Schema(description = "用户类型：0-超级管理员，1-普通管理员，2-普通用户")
    private Integer type;

    @Schema(description = "账号状态：0-正常，1-封禁")
    private Integer status;

    @Schema(description = "注册时间开始")
    private LocalDateTime createdAtStart;

    @Schema(description = "注册时间结束")
    private LocalDateTime createdAtEnd;
}
package org.catmunity.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端用户视图对象")
public class UserAdminVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户头像URL")
    private String avatar;

    @Schema(description = "用户账号/昵称")
    private String identifier;

    @Schema(description = "用户手机号")
    private String phone;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "用户类型：0-超级管理员，1-普通管理员，2-普通用户")
    private Integer type;

    @Schema(description = "用户类型描述")
    private String typeStr;

    @Schema(description = "账号状态：0-正常，1-封禁")
    private Integer status;

    @Schema(description = "账号状态描述")
    private String statusStr;

    @Schema(description = "封禁原因")
    private String banReason;

    @Schema(description = "封禁时间")
    private LocalDateTime bannedAt;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;

    @Schema(description = "最近活跃时间")
    private LocalDateTime lastActive;

    @Schema(description = "发帖数量")
    private Long postCount;

    @Schema(description = "打卡数量")
    private Long checkinCount;

    @Schema(description = "关注数量")
    private Long followCount;

    @Schema(description = "粉丝数量")
    private Long followerCount;
}
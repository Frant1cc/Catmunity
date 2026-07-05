package org.catmunity.Enum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "审核状态枚举")
public enum ModerationStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    AUTO_FLAGGED(3, "AI标记待复核");

    private final Integer code;
    private final String description;

    public static ModerationStatus fromCode(Integer code) {
        if (code == null) return null;
        for (ModerationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
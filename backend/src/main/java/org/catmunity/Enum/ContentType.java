package org.catmunity.Enum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "内容类型枚举")
public enum ContentType {

    POST(1, "帖子"),
    COMMENT(2, "评论");

    private final Integer code;
    private final String description;

    public static ContentType fromCode(Integer code) {
        if (code == null) return null;
        for (ContentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
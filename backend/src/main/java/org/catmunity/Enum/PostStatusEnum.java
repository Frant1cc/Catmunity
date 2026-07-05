package org.catmunity.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostStatusEnum {
    NORMAL(0, "正常"),
    DELETED(1, "已删除"),
    BLOCKED(2, "已封禁");

    private final Integer code;
    private final String description;
}
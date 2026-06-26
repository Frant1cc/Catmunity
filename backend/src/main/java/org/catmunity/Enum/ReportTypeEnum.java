package org.catmunity.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportTypeEnum {
    SPAM(1, "垃圾广告"),
    PORN(2, "色情低俗"),
    VIOLENCE(3, "暴力血腥"),
    ILLEGAL(4, "违法犯罪"),
    FALSE_INFO(5, "虚假信息"),
    PERSONAL_ATTACK(6, "人身攻击"),
    OTHER(99, "其他");

    private final Integer code;
    private final String description;
}
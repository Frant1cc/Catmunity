package org.catmunity.Enum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "违规类型枚举")
public enum ViolationType {

    PORNOGRAPHY(1, "色情低俗", List.of("涉黄内容", "性暗示", "裸露")),
    VIOLENCE(2, "暴力血腥", List.of("暴力画面", "血腥内容", "伤害威胁")),
    ADVERTISING(3, "广告推广", List.of("垃圾广告", "推广链接", "诱导分享")),
    POLITICAL(4, "政治敏感", List.of("政治言论", "敏感事件")),
    FRAUD(5, "欺诈诈骗", List.of("诈骗信息", "虚假宣传", "钓鱼链接")),
    PERSONAL_ATTACK(6, "人身攻击", List.of("侮辱谩骂", "诽谤诋毁", "隐私泄露")),
    SPAM(7, "垃圾信息", List.of("刷屏重复", "无意义内容", "恶意灌水")),
    OTHER(99, "其他违规", List.of("其他违规内容"));

    private final Integer code;
    private final String description;
    private final List<String> keywords;

    public static ViolationType fromCode(Integer code) {
        if (code == null) return null;
        for (ViolationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
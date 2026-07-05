package org.catmunity.Enum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "风险等级枚举")
public enum RiskLevel {

    SAFE(0, "安全", "#52c41a"),
    LOW(1, "低风险", "#faad14"),
    MEDIUM(2, "中风险", "#fa8c16"),
    HIGH(3, "高风险", "#f5222d"),
    VERY_HIGH(4, "极高风险", "#cf1322");

    private final Integer code;
    private final String description;
    private final String color;

    public static RiskLevel fromCode(Integer code) {
        if (code == null) return SAFE;
        for (RiskLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return SAFE;
    }

    public static RiskLevel fromScore(Double score) {
        if (score == null) return SAFE;
        if (score >= 0.8) return VERY_HIGH;
        if (score >= 0.6) return HIGH;
        if (score >= 0.4) return MEDIUM;
        if (score >= 0.2) return LOW;
        return SAFE;
    }
}
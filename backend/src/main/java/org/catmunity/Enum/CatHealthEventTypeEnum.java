package org.catmunity.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CatHealthEventTypeEnum {
    VACCINATION(1, "疫苗接种"),
    DEWORMING(2, "驱虫"),
    NEUTERING(3, "绝育"),
    CHECKUP(4, "体检"),
    ILLNESS(5, "疾病治疗"),
    GROOMING(6, "美容护理"),
    OTHER(99, "其他");

    private final Integer code;
    private final String description;

    public static CatHealthEventTypeEnum fromCode(Integer code) {
        if (code == null) {
            return OTHER;
        }
        for (CatHealthEventTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
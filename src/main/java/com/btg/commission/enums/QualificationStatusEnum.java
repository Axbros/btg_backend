package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum QualificationStatusEnum {

    /** 待系统管理员资格审核 */
    PENDING(1),
    /** 已通过 */
    APPROVED(2),
    /** 已拒绝 */
    REJECTED(3);

    @EnumValue
    @JsonValue
    private final int code;

    QualificationStatusEnum(int code) {
        this.code = code;
    }

    public static QualificationStatusEnum fromCode(int code) {
        for (QualificationStatusEnum v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown QualificationStatusEnum: " + code);
    }
}

package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum KycStatus {

    NOT_SUBMITTED(0),
    PENDING(1),
    APPROVED(2),
    REJECTED(3);

    @EnumValue
    @JsonValue
    private final int code;

    KycStatus(int code) {
        this.code = code;
    }

    public static KycStatus fromCode(int code) {
        for (KycStatus v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown KycStatus: " + code);
    }
}

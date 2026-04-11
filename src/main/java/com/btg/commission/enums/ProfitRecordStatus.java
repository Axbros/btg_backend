package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProfitRecordStatus {

    PENDING(1),
    APPROVED(2),
    REJECTED(3);

    @EnumValue
    @JsonValue
    private final int code;

    ProfitRecordStatus(int code) {
        this.code = code;
    }

    public static ProfitRecordStatus fromCode(int code) {
        for (ProfitRecordStatus v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown ProfitRecordStatus: " + code);
    }
}

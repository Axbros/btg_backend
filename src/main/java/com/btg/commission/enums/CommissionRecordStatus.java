package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CommissionRecordStatus {

    CONFIRMED(1);

    @EnumValue
    @JsonValue
    private final int code;

    CommissionRecordStatus(int code) {
        this.code = code;
    }

    public static CommissionRecordStatus fromCode(int code) {
        for (CommissionRecordStatus v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown CommissionRecordStatus: " + code);
    }
}

package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BindingStatus {

    INACTIVE(0),
    ACTIVE(1);

    @EnumValue
    @JsonValue
    private final int code;

    BindingStatus(int code) {
        this.code = code;
    }

    public static BindingStatus fromCode(int code) {
        for (BindingStatus v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown BindingStatus: " + code);
    }
}

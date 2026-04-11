package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatus {

    DISABLED(0),
    NORMAL(1);

    @EnumValue
    @JsonValue
    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus: " + code);
    }
}

package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum StrategyStatus {

    DISABLED(0),
    ENABLED(1);

    @EnumValue
    @JsonValue
    private final int code;

    StrategyStatus(int code) {
        this.code = code;
    }

    public static StrategyStatus fromCode(int code) {
        for (StrategyStatus v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown StrategyStatus: " + code);
    }
}

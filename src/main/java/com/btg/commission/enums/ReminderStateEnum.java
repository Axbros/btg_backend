package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum ReminderStateEnum implements IEnum<String> {

    OPEN("OPEN"),
    DONE("DONE"),
    CANCELLED("CANCELLED");

    private final String code;

    ReminderStateEnum(String code) {
        this.code = code;
    }

    @Override
    public String getValue() {
        return code;
    }
}

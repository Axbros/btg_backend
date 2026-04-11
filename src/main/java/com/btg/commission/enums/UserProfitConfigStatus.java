package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum UserProfitConfigStatus implements IEnum<Integer> {

    ACTIVE(1),
    INACTIVE(0);

    private final int code;

    UserProfitConfigStatus(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}

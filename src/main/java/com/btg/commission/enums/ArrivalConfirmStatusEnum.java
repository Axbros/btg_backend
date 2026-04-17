package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 补仓到账确认状态，对应 {@code btg_replenishment_apply.arrival_confirm_status}。
 */
@Getter
public enum ArrivalConfirmStatusEnum implements IEnum<Integer> {

    PENDING(1),
    CONFIRMED(2),
    REJECTED(3);

    private final int code;

    ArrivalConfirmStatusEnum(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}

package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 逐级结算单：1 INIT；2 待提交凭证；3 待上级审核；4 通过；5 拒绝。
 */
@Getter
public enum SettlementOrderStatus implements IEnum<Integer> {

    INIT(1),
    PENDING_SUBMIT(2),
    PENDING_REVIEW(3),
    APPROVED(4),
    REJECTED(5);

    private final int code;

    SettlementOrderStatus(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}

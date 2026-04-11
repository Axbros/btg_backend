package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 补仓申请状态：1 待审核；2 审核通过；3 审核拒绝；4 部分归还；5 已结清；6 已关闭。
 */
@Getter
public enum ReplenishmentStatusEnum implements IEnum<Integer> {

    PENDING_AUDIT(1),
    APPROVED(2),
    REJECTED(3),
    PARTIALLY_REPAID(4),
    FULLY_REPAID(5),
    CLOSED(6);

    private final int code;

    ReplenishmentStatusEnum(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}

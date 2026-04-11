package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 归仓申请状态：1 待审核；2 审核通过；3 审核拒绝。
 */
@Getter
public enum RepayStatusEnum implements IEnum<Integer> {

    PENDING_AUDIT(1),
    APPROVED(2),
    REJECTED(3);

    private final int code;

    RepayStatusEnum(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}

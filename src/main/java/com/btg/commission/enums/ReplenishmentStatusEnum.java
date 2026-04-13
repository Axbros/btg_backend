package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 补仓申请状态：1 待受理；7 已受理待资方上传凭证与备注；8 待资方终审确认；
 * 2 审核通过；3 审核拒绝；4 部分归还；5 已结清；6 已关闭。
 */
@Getter
public enum ReplenishmentStatusEnum implements IEnum<Integer> {

    PENDING_AUDIT(1),
    APPROVED(2),
    REJECTED(3),
    PARTIALLY_REPAID(4),
    FULLY_REPAID(5),
    CLOSED(6),
    /** 资方已受理，待根用户上传转账凭证与备注（展示申请人交易所/钱包） */
    PENDING_SUPPLEMENT(7),
    /** 已上传资方凭证，待资方终审确认 */
    PENDING_TRANSFER(8);

    private final int code;

    ReplenishmentStatusEnum(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}

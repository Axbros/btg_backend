package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 利润上报状态：1 待直属上级审核；2 首级结算通过/已进入结算链；3 历史终局拒绝（兼容旧数据）；
 * 4 全链路完成；5 已退回发起人待修改（可重提）。
 */
@Getter
public enum ProfitReportStatus implements IEnum<Integer> {

    PENDING_DIRECT_REVIEW(1),
    IN_SETTLEMENT_CHAIN(2),
    REJECTED(3),
    ALL_COMPLETED(4),
    /** 被上级/链上拒绝后退回申报人，可修改后重提 */
    RETURNED_TO_APPLICANT(5);

    private final int code;

    ProfitReportStatus(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}

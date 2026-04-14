package com.btg.commission.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 利润链「层级」聚合状态（不含结算单明细与审核流水）。
 */
@Getter
public enum ProfitFlowLayerState {

    /** 利润单：待直属上级审核 */
    PENDING_DIRECT_REVIEW,
    /** 利润单：已退回申报人修改 */
    RETURNED_TO_APPLICANT,
    /** 利润单：历史终局拒绝 */
    PROFIT_REJECTED,
    /** 利润单：已进入结算链（直属已通过） */
    DIRECT_REVIEW_PASSED,
    /** 结算：尚未轮到该层 */
    SETTLEMENT_NOT_STARTED,
    /** 结算：付款人待提交转账凭证 */
    SETTLEMENT_PENDING_SUBMIT,
    /** 结算：待收款上级审核 */
    SETTLEMENT_PENDING_REVIEW,
    /** 结算：已通过 */
    SETTLEMENT_APPROVED,
    /** 结算：已拒绝 */
    SETTLEMENT_REJECTED;

    @JsonValue
    public String toJson() {
        return name();
    }
}

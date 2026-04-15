package com.btg.commission.enums;

import lombok.Getter;

/**
 * 首页待办聚合项类型
 */
@Getter
public enum DashboardTodoType {

    SETTLEMENT_PAYABLE,
    SETTLEMENT_REVIEW,
    PROFIT_REPORT_REVIEW,
    /** 非直属上级的祖先：下级利润单在链路上的只读关注（不替代结算/待审操作项） */
    PROFIT_REPORT_CHAIN_WATCH,
    REPLENISHMENT_REVIEW,
    REPAY_REVIEW,
    PROFIT_REPORT_RETURNED,
    REPLENISHMENT_RETURNED,
    REPAY_RETURNED
}

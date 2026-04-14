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
    REPLENISHMENT_REVIEW,
    REPAY_REVIEW,
    PROFIT_REPORT_RETURNED,
    REPLENISHMENT_RETURNED,
    REPAY_RETURNED
}

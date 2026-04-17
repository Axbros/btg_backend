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
    /** 系统管理员：待审核补仓 */
    REPLENISHMENT_ADMIN_REVIEW,
    /** 资方执行人：待提交补仓凭证 / 被退回修改 */
    REPLENISHMENT_CAPITAL_SUBMIT,
    /** 申请人：待确认到账 */
    REPLENISHMENT_APPLICANT_CONFIRM,
    /** 链路上级：下级补仓进行中（只读） */
    REPLENISHMENT_CHAIN_WATCH,
    /** 资方执行人：待审核归仓申请 */
    REPLENISHMENT_REPAY_CAPITAL_REVIEW,
    /** 链路上级：下级归仓流程（只读） */
    REPLENISHMENT_REPAY_CHAIN_WATCH,
    PROFIT_REPORT_RETURNED,
    REPLENISHMENT_RETURNED,
    /** 申请人：归仓被退回待修改重提 */
    REPLENISHMENT_REPAY_RETURNED_TO_APPLICANT
}

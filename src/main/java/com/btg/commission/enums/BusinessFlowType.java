package com.btg.commission.enums;

import lombok.Getter;

/**
 * {@code btg_business_flow_log.business_type}，与库中 VARCHAR 一致（{@link #name()}）。
 */
@Getter
public enum BusinessFlowType {

    PROFIT_REPORT,
    REPLENISHMENT_APPLY,
    REPLENISHMENT_REPAY_APPLY,
    SETTLEMENT_ORDER
}

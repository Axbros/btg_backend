package com.btg.commission.enums;

import lombok.Getter;

/**
 * 流转动作（落库为字符串）
 */
@Getter
public enum FlowAction {

    SUBMIT,
    APPROVE,
    REJECT,
    /** 拒绝并退回发起人修改 */
    RETURN_TO_APPLICANT,
    RESUBMIT,
    /** 进入结算链等系统动作 */
    ADVANCE,
    CANCEL
}

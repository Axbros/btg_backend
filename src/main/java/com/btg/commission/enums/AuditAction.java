package com.btg.commission.enums;

import lombok.Getter;

@Getter
public enum AuditAction {

    SUBMIT("SUBMIT"),
    APPROVE("APPROVE"),
    REJECT("REJECT"),
    /** 补仓：管理员转派资方执行人 */
    ASSIGN("ASSIGN"),
    /** 用户资格审核重新提交（资格状态 REJECTED → PENDING） */
    RESUBMIT("RESUBMIT"),
    REGISTER("REGISTER"),
    BIND_STRATEGY("BIND_STRATEGY"),
    /** WebView 网关配置更新 */
    UPDATE("UPDATE");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }
}

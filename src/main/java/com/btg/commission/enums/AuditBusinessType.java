package com.btg.commission.enums;

import lombok.Getter;

@Getter
public enum AuditBusinessType {

    PROFIT_RECORD("PROFIT_RECORD"),
    PROFIT_REPORT("PROFIT_REPORT"),
    SETTLEMENT_ORDER("SETTLEMENT_ORDER"),
    USER("USER"),
    COMMISSION_BINDING("COMMISSION_BINDING"),
    USER_PROFILE_KYC("USER_PROFILE_KYC"),
    /** 新成员系统管理员资格审核（btg_user_profile.qualification_*） */
    USER_QUALIFICATION("USER_QUALIFICATION"),
    /** 分润模式切换审核（直属上级提审，根用户审批） */
    PROFIT_CONFIG_MODE("PROFIT_CONFIG_MODE"),
    REPLENISHMENT_APPLY("REPLENISHMENT_APPLY"),
    REPLENISHMENT_REPAY("REPLENISHMENT_REPAY"),
    /** 安卓 WebView 网关单表配置 */
    WEBVIEW_CONFIG("WEBVIEW_CONFIG");

    private final String code;

    AuditBusinessType(String code) {
        this.code = code;
    }
}

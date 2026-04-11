package com.btg.commission.enums;

import lombok.Getter;

@Getter
public enum AuditBusinessType {

    PROFIT_RECORD("PROFIT_RECORD"),
    PROFIT_REPORT("PROFIT_REPORT"),
    SETTLEMENT_ORDER("SETTLEMENT_ORDER"),
    USER("USER"),
    COMMISSION_BINDING("COMMISSION_BINDING"),
    USER_PROFILE_KYC("USER_PROFILE_KYC");

    private final String code;

    AuditBusinessType(String code) {
        this.code = code;
    }
}

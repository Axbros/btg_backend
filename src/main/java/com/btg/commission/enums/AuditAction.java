package com.btg.commission.enums;

import lombok.Getter;

@Getter
public enum AuditAction {

    SUBMIT("SUBMIT"),
    APPROVE("APPROVE"),
    REJECT("REJECT"),
    REGISTER("REGISTER"),
    BIND_STRATEGY("BIND_STRATEGY");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }
}

package com.btg.commission.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProfitConfigAuditStatus {

    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    @JsonValue
    private final String code;

    ProfitConfigAuditStatus(String code) {
        this.code = code;
    }

    @JsonCreator
    public static ProfitConfigAuditStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim();
        for (ProfitConfigAuditStatus v : values()) {
            if (v.code.equalsIgnoreCase(c)) {
                return v;
            }
        }
        return null;
    }
}

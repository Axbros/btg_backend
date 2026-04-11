package com.btg.commission.enums;

import lombok.Getter;

@Getter
public enum FileAttachmentBusinessType {

    USER_PROFILE("USER_PROFILE"),
    PROFIT_RECORD("PROFIT_RECORD");

    private final String code;

    FileAttachmentBusinessType(String code) {
        this.code = code;
    }
}

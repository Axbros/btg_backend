package com.btg.commission.enums;

import lombok.Getter;

@Getter
public enum ProfitAttachmentFileType {

    PROFIT("PROFIT"),
    TRANSFER("TRANSFER");

    private final String code;

    ProfitAttachmentFileType(String code) {
        this.code = code;
    }
}

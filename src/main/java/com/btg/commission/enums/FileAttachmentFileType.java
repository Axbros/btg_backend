package com.btg.commission.enums;

import lombok.Getter;

@Getter
public enum FileAttachmentFileType {

    ID_CARD_FRONT("ID_CARD_FRONT"),
    ID_CARD_BACK("ID_CARD_BACK"),
    FACE("FACE"),
    PROFIT("PROFIT"),
    TRANSFER("TRANSFER"),
    OTHER("OTHER");

    private final String code;

    FileAttachmentFileType(String code) {
        this.code = code;
    }
}

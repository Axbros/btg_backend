package com.btg.commission.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 直属上级为下级配置的当前生效分润模式（利润上报不由此字段由用户选择，由配置快照决定）。
 */
@Getter
@Schema(description = "分润模式：兜底 / 不兜底")
public enum CommissionModeEnum {

    /** 兜底 */
    GUARANTEE("GUARANTEE"),
    /** 不兜底 */
    NON_GUARANTEE("NON_GUARANTEE");

    @JsonValue
    private final String code;

    CommissionModeEnum(String code) {
        this.code = code;
    }

    @JsonCreator
    public static CommissionModeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim();
        for (CommissionModeEnum v : values()) {
            if (v.code.equalsIgnoreCase(c)) {
                return v;
            }
        }
        return null;
    }

    /** 中文描述，供 VO 展示 */
    public String getDescription() {
        return switch (this) {
            case GUARANTEE -> "兜底";
            case NON_GUARANTEE -> "不兜底";
        };
    }

    /** 根据库存字符串返回中文描述；未知则 null */
    public static String descriptionOrNull(String storedCode) {
        CommissionModeEnum e = fromCode(storedCode);
        return e == null ? null : e.getDescription();
    }
}

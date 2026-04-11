package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatus {

    /** 注册后待完善资料；仅可提交 {@code btg_user_profile} */
    PROFILE_INCOMPLETE(-1),
    /** 已提交资料，待直属上级审核 */
    PENDING_APPROVAL(0),
    /** 审核通过，全功能可用；不可再改资料 */
    NORMAL(1);

    @EnumValue
    @JsonValue
    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus: " + code);
    }

    /** 允许登录（未删除用户且为 -1 / 0 / 1） */
    public static boolean canAuthenticate(UserStatus s) {
        if (s == null) {
            return false;
        }
        return s == PROFILE_INCOMPLETE || s == PENDING_APPROVAL || s == NORMAL;
    }

    /** 可作邀请人：仅审核通过 */
    public static boolean canInviteOthers(UserStatus s) {
        return s == NORMAL;
    }
}

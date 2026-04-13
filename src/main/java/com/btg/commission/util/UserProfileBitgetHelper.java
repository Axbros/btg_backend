package com.btg.commission.util;

import com.btg.commission.entity.UserProfile;
import org.springframework.util.StringUtils;

/**
 * Bitget 资料在接口层的展示字段（不落库）。
 */
public final class UserProfileBitgetHelper {

    private UserProfileBitgetHelper() {
    }

    public static boolean isBitgetConfigured(UserProfile p) {
        if (p == null) {
            return false;
        }
        return StringUtils.hasText(p.getBitgetAccessKey())
                && StringUtils.hasText(p.getBitgetSecretKey())
                && StringUtils.hasText(p.getBitgetPassphrase());
    }

    /** 写入 {@link UserProfile} 的 exist=false 展示字段 */
    public static void applyPresentation(UserProfile p) {
        if (p == null) {
            return;
        }
        p.setBitgetConfigured(isBitgetConfigured(p));
        p.setAccessKeyMasked(ApiKeyMaskUtil.maskAccessKey(p.getBitgetAccessKey()));
    }
}

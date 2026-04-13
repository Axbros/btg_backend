package com.btg.commission.util;

import org.springframework.util.StringUtils;

/**
 * API Key 掩码展示；不记录日志、不还原原文。
 */
public final class ApiKeyMaskUtil {

    private ApiKeyMaskUtil() {
    }

    /**
     * 长度 ≤ 8 返回 {@code ****}；否则前 4 + **** + 后 4。
     */
    public static String maskAccessKey(String accessKey) {
        if (!StringUtils.hasText(accessKey)) {
            return null;
        }
        String s = accessKey.trim();
        if (s.length() <= 8) {
            return "****";
        }
        return s.substring(0, 4) + "****" + s.substring(s.length() - 4);
    }
}

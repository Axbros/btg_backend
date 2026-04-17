package com.btg.commission.util;

import com.btg.commission.entity.BtgUser;

public final class AncestorPathUtil {

    private AncestorPathUtil() {
    }

    /**
     * Child ancestor_path = normalize(referrer.ancestor_path) + referrer.id + '/'
     * 与 {@link #descendantPathPrefix}、团队 descendants 前缀一致；空白/仅空格视为 {@code "/"}。
     */
    public static String buildChildAncestorPath(BtgUser referrer) {
        if (referrer == null || referrer.getId() == null) {
            throw new IllegalArgumentException("referrer required");
        }
        String base = normalizeAncestorPathBase(referrer.getAncestorPath());
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + referrer.getId() + "/";
    }

    /** 上级链前缀：空、null、全空白 → {@code "/"}，否则 trim。 */
    public static String normalizeAncestorPathBase(String raw) {
        if (raw == null) {
            return "/";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "/";
        }
        return t;
    }

    /**
     * Prefix to query all descendants (users under this node in referral tree).
     */
    public static String descendantPathPrefix(BtgUser user) {
        return buildChildAncestorPath(user);
    }
}

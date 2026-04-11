package com.btg.commission.util;

import com.btg.commission.entity.SysUser;

public final class AncestorPathUtil {

    private AncestorPathUtil() {
    }

    /**
     * Child ancestor_path = referrer.ancestor_path + referrer.id + '/'
     * Root referrer with id 1 and path '/' yields '/1/'.
     */
    public static String buildChildAncestorPath(SysUser referrer) {
        if (referrer == null || referrer.getId() == null) {
            throw new IllegalArgumentException("referrer required");
        }
        String base = referrer.getAncestorPath() == null ? "/" : referrer.getAncestorPath();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + referrer.getId() + "/";
    }

    /**
     * Prefix to query all descendants (users under this node in referral tree).
     */
    public static String descendantPathPrefix(SysUser user) {
        return buildChildAncestorPath(user);
    }
}

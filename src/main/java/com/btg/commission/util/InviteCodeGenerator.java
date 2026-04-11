package com.btg.commission.util;

import java.security.SecureRandom;

public final class InviteCodeGenerator {

    private static final char[] ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RND = new SecureRandom();

    private InviteCodeGenerator() {
    }

    public static String random(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHANUM[RND.nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
    }
}

package com.btg.commission.util;

import java.math.BigDecimal;

/**
 * 金额/指标比较：仅当两者均为 null 时视为相等；否则用 {@link BigDecimal#compareTo}。
 */
public final class BigDecimalCompare {

    private BigDecimalCompare() {
    }

    /**
     * {@code null} 与 {@code null} 为相等；任一为 null 或与另一数值 compareTo != 0 则为不相等。
     */
    public static boolean sameValue(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }
}

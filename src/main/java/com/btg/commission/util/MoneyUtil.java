package com.btg.commission.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额与比例运算一律使用 {@link BigDecimal}，禁止用 double / float 参与金额计算。
 * 实体、DTO、VO 中的金额字段均应为 {@link BigDecimal}，经本类归一化刻度后再落库或返回。
 */
public final class MoneyUtil {

    public static final int MONEY_SCALE = 2;
    public static final int RATE_SCALE = 4;
    public static final int RATIO_SCALE = 6;

    private MoneyUtil() {
    }

    public static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal rate(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP)
                : v.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    /** 父子分润绝对比例（与库 decimal(10,6) 一致） */
    public static BigDecimal profitRatio(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(RATIO_SCALE, RoundingMode.HALF_UP)
                : v.setScale(RATIO_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal sliceIncome(BigDecimal profit, BigDecimal upperRatio, BigDecimal lowerRatio) {
        BigDecimal p = money(profit);
        BigDecimal u = profitRatio(upperRatio);
        BigDecimal l = profitRatio(lowerRatio);
        return money(p.multiply(u.subtract(l)));
    }

    public static BigDecimal payToParentAmount(BigDecimal profit, BigDecimal childLineRatio) {
        BigDecimal p = money(profit);
        BigDecimal r = profitRatio(childLineRatio);
        return money(p.multiply(BigDecimal.ONE.subtract(r)));
    }

    /** 申报人自留：盈利 × 分佣比例；与 {@link #dueShareAmount}（分给上级）互补。 */
    public static BigDecimal commissionOf(BigDecimal profitAmount, BigDecimal rate) {
        return money(profitAmount.multiply(rate(rate)));
    }

    /** 分给上级：总盈利 × (1 − 分佣比例)；存 {@code commission_amount}；待审分出/上级待审应收。 */
    public static BigDecimal dueShareAmount(BigDecimal profitAmount, BigDecimal rate) {
        BigDecimal r = rate(rate);
        return money(profitAmount.multiply(BigDecimal.ONE.subtract(r)));
    }

    /** 同 {@link #commissionOf}；存申报单 {@code net_amount}；申报人待审应收。 */
    public static BigDecimal commissionShareOfProfit(BigDecimal profitAmount, BigDecimal rate) {
        return commissionOf(profitAmount, rate);
    }
}

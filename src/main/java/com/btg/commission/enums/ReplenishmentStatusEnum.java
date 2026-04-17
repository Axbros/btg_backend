package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 补仓申请状态（整型与库字段 {@code btg_replenishment_apply.status} 一致）。
 * <p>旧码 1～9 已通过 SQL 迁移至本枚举；归仓仍依赖「成功态」{@link #SUCCESS} 及剩余金额字段。</p>
 */
@Getter
public enum ReplenishmentStatusEnum implements IEnum<Integer> {

    /** 待系统管理员审核 */
    PENDING_ADMIN_REVIEW(1),
    /**
     * 管理员已通过申请、待转派或已记录资方归属；{@code assigned_capital_user_id} 为空时表示待管理员转派执行人
     */
    ASSIGNED_TO_CAPITAL(2),
    /** 待资方执行人提交补仓转账凭证 */
    PENDING_CAPITAL_SUBMIT(3),
    /** 待申请人确认到账 */
    PENDING_APPLICANT_CONFIRM(4),
    /** 申请人拒绝到账，退回资方执行人修改凭证 */
    RETURNED_TO_CAPITAL(5),
    /** 申请人已确认到账，补仓成功（可归仓直至剩余为 0） */
    SUCCESS(6),
    /** 管理员拒绝或其他终局拒绝 */
    REJECTED(7),
    /** 关闭（如全额归还后结案等） */
    CLOSED(8);

    private final int code;

    ReplenishmentStatusEnum(int code) {
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    /** 是否阻断新的补仓申请（与利润上报「未完成补仓」口径一致，含 SUCCESS 且仍有剩余应还） */
    public static boolean blocksNewReplenishmentOrProfit(ReplenishmentStatusEnum s, java.math.BigDecimal remainingAmount) {
        if (s == null) {
            return false;
        }
        if (s == SUCCESS) {
            java.math.BigDecimal r = remainingAmount == null ? java.math.BigDecimal.ZERO : remainingAmount;
            return r.compareTo(java.math.BigDecimal.ZERO) > 0;
        }
        return s == PENDING_ADMIN_REVIEW
                || s == ASSIGNED_TO_CAPITAL
                || s == PENDING_CAPITAL_SUBMIT
                || s == PENDING_APPLICANT_CONFIRM
                || s == RETURNED_TO_CAPITAL;
    }

    public static boolean isTerminal(ReplenishmentStatusEnum s) {
        return s == SUCCESS || s == REJECTED || s == CLOSED;
    }
}

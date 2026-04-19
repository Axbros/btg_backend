package com.btg.commission.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 补仓申请状态（整型与库字段 {@code btg_replenishment_apply.status} 一致）。
 * <p>与前端约定：1 待管理员审核；2 已通过待转派（仅兼容历史数据，新流程不再写入）；3 待资方提交；4 待确认到账；
 * 5 已退回资方；6 补仓成功；7 已拒绝；8 已关闭。</p>
 * <p>新流程：提交 →1；管理员同意并上传凭证 →4；管理员转派资方 →3；资方提交凭证 →4；申请人拒绝到账 →5；
 * 资方拒绝执行 → 回到 1（待管理员再次同意/转派）；管理员拒绝 →7；申请人确认到账 →6。</p>
 */
@Getter
public enum ReplenishmentStatusEnum implements IEnum<Integer> {

    /** 1 待管理员审核（新单；或资方拒绝执行后回到此态） */
    PENDING_ADMIN_REVIEW(1),
    /**
     * 2 已通过待转派（仅历史/兼容；新流程不再进入。{@link #assignCapital} 仍允许从本状态转派以消化旧数据）
     */
    ASSIGNED_TO_CAPITAL(2),
    /** 3 待资方提交（管理员已转派执行人，待上传转账凭证） */
    PENDING_CAPITAL_SUBMIT(3),
    /** 4 待确认到账（资方或管理员已提交凭证，待申请人确认） */
    PENDING_APPLICANT_CONFIRM(4),
    /** 5 已退回资方（申请人拒绝到账，执行人须重传凭证） */
    RETURNED_TO_CAPITAL(5),
    /** 6 补仓成功 */
    SUCCESS(6),
    /** 7 已拒绝 */
    REJECTED(7),
    /** 8 已关闭 */
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

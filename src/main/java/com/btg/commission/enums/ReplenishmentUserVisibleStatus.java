package com.btg.commission.enums;

import lombok.Getter;

import java.util.List;

/**
 * 申请人「我的补仓」列表简化状态（与 {@code ReplenishmentApplyBriefVO#userVisibleStatus} 整型一致）。
 * <p>后台 {@link ReplenishmentStatusEnum} 映射：1/2/3/5 → 本枚举 1 待审核；4 → 2 待确认；6→3 成功；7→4 拒绝；8→5 关闭。</p>
 */
@Getter
public enum ReplenishmentUserVisibleStatus {

    /** 1：待审核（含后台待管理员审核、历史待转派、待资方提交、已退回资方） */
    PENDING_REVIEW(1),
    /** 2：待确认到账 */
    AWAITING_CONFIRM(2),
    /** 3：已成功 */
    SUCCESS(3),
    /** 4：已拒绝 */
    REJECTED(4),
    /** 5：已关闭 */
    CLOSED(5);

    private final int code;

    ReplenishmentUserVisibleStatus(int code) {
        this.code = code;
    }

    /**
     * 将补仓主状态转为申请人列表展示用状态；未知状态返回 null。
     */
    public static Integer codeForApplicantList(ReplenishmentStatusEnum backend) {
        if (backend == null) {
            return null;
        }
        return switch (backend) {
            case PENDING_ADMIN_REVIEW, ASSIGNED_TO_CAPITAL, PENDING_CAPITAL_SUBMIT, RETURNED_TO_CAPITAL ->
                    PENDING_REVIEW.code;
            case PENDING_APPLICANT_CONFIRM -> AWAITING_CONFIRM.code;
            case SUCCESS -> SUCCESS.code;
            case REJECTED -> REJECTED.code;
            case CLOSED -> CLOSED.code;
        };
    }

    /**
     * 「我的」列表按申请人简化状态筛选时对应的后台主状态集合；非法 code 返回 null。
     */
    public static List<ReplenishmentStatusEnum> backendStatusesForMineFilter(int userVisibleStatus) {
        return switch (userVisibleStatus) {
            case 1 -> List.of(
                    ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW,
                    ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL,
                    ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT,
                    ReplenishmentStatusEnum.RETURNED_TO_CAPITAL);
            case 2 -> List.of(ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM);
            case 3 -> List.of(ReplenishmentStatusEnum.SUCCESS);
            case 4 -> List.of(ReplenishmentStatusEnum.REJECTED);
            case 5 -> List.of(ReplenishmentStatusEnum.CLOSED);
            default -> null;
        };
    }
}

package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.enums.ReplenishmentStatusEnum;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code deleted_at IS NULL} 由实体 {@link com.baomidou.mybatisplus.annotation.TableLogic} 自动追加。
 */
public interface BtgReplenishmentApplyMapper extends BaseMapper<BtgReplenishmentApply> {

    List<ReplenishmentStatusEnum> BLOCKING_PROGRESS_STATUSES = List.of(
            ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW,
            ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL,
            ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT,
            ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM,
            ReplenishmentStatusEnum.RETURNED_TO_CAPITAL);

    /**
     * 存在未结清补仓：SUCCESS 且仍有剩余应还；与利润上报「未完成补仓」一致。
     */
    default boolean existsOpenByUserId(Long userId) {
        return existsBlockingReplenishmentForUser(userId);
    }

    /**
     * 禁止利润上报：进行中状态或已成功但仍有剩余未还。
     */
    default boolean existsBlockingReplenishmentForUser(Long userId) {
        if (userId == null) {
            return false;
        }
        Long c = selectCount(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .and(w -> w.in(BtgReplenishmentApply::getStatus, BLOCKING_PROGRESS_STATUSES)
                        .or(sub -> sub.eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.SUCCESS)
                                .isNotNull(BtgReplenishmentApply::getRemainingAmount)
                                .gt(BtgReplenishmentApply::getRemainingAmount, BigDecimal.ZERO))));
        return c != null && c > 0;
    }

    /**
     * 禁止重复发起补仓：存在进行中申请，或已成功且仍有未结清金额。
     */
    default boolean existsBlockingNewReplenishmentByUserId(Long userId) {
        return existsBlockingReplenishmentForUser(userId);
    }
}

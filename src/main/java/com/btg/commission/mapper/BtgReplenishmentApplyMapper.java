package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.enums.ReplenishmentStatusEnum;

/**
 * {@code deleted_at IS NULL} 由实体 {@link com.baomidou.mybatisplus.annotation.TableLogic} 自动追加。
 */
public interface BtgReplenishmentApplyMapper extends BaseMapper<BtgReplenishmentApply> {

    /**
     * 利润上报拦截：存在未结清补仓（审核通过或部分归还）。
     * 条件：{@code status IN (2,4)}。
     */
    default boolean existsOpenByUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        Long c = selectCount(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .in(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.APPROVED, ReplenishmentStatusEnum.PARTIALLY_REPAID));
        return c != null && c > 0;
    }

    /**
     * 禁止重复提交补仓：待审核、已通过、部分归还均不可再发起新单。
     */
    default boolean existsBlockingNewReplenishmentByUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        Long c = selectCount(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .in(BtgReplenishmentApply::getStatus,
                        ReplenishmentStatusEnum.PENDING_AUDIT,
                        ReplenishmentStatusEnum.APPROVED,
                        ReplenishmentStatusEnum.PARTIALLY_REPAID));
        return c != null && c > 0;
    }
}

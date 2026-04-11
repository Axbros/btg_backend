package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.UserAccountSummary;
import com.btg.commission.mapper.UserAccountSummaryMapper;
import com.btg.commission.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserAccountSummaryService {

    private final UserAccountSummaryMapper userAccountSummaryMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public UserAccountSummary lockByUserId(Long userId) {
        UserAccountSummary row = userAccountSummaryMapper.selectByUserIdForUpdate(userId);
        if (row == null) {
            UserAccountSummary init = newEmptySummary(userId);
            userAccountSummaryMapper.insert(init);
            row = userAccountSummaryMapper.selectByUserIdForUpdate(userId);
        }
        return row;
    }

    public UserAccountSummary getOrEmpty(Long userId) {
        UserAccountSummary row = userAccountSummaryMapper.selectOne(new LambdaQueryWrapper<UserAccountSummary>()
                .eq(UserAccountSummary::getUserId, userId)
                .last("LIMIT 1"));
        if (row == null) {
            return newEmptySummary(userId);
        }
        return row;
    }

    private UserAccountSummary newEmptySummary(Long userId) {
        UserAccountSummary s = new UserAccountSummary();
        s.setUserId(userId);
        s.setTotalProfitAmount(MoneyUtil.money(BigDecimal.ZERO));
        s.setTotalCommissionOutAmount(MoneyUtil.money(BigDecimal.ZERO));
        s.setTotalCommissionInAmount(MoneyUtil.money(BigDecimal.ZERO));
        s.setPendingCommissionOutAmount(MoneyUtil.money(BigDecimal.ZERO));
        s.setPendingCommissionInAmount(MoneyUtil.money(BigDecimal.ZERO));
        return s;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void persist(UserAccountSummary row) {
        userAccountSummaryMapper.updateById(row);
    }

    /**
     * 收益申报提交后累加 pending（须已按 min/max 顺序对双方 {@link #lockByUserId}）。
     * 下级 pending_out += 分给上级部分（盈利×(1−比例)）；下级 pending_in += 本人分成（盈利×比例）；
     * 上级 pending_in += 收下级的同上「分给上级」部分。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyPendingOnProfitSubmit(Long childUserId, Long referrerUserId,
            BigDecimal childPendingOutToReferrer,
            BigDecimal childPendingInRetained) {
        BigDecimal out = MoneyUtil.money(childPendingOutToReferrer);
        BigDecimal childIn = MoneyUtil.money(childPendingInRetained);
        BigDecimal refIn = MoneyUtil.money(childPendingOutToReferrer);
        int n1 = userAccountSummaryMapper.addPendingCommissionOut(childUserId, out);
        int n2 = userAccountSummaryMapper.addPendingCommissionIn(childUserId, childIn);
        int n3 = userAccountSummaryMapper.addPendingCommissionIn(referrerUserId, refIn);
        if (n1 != 1 || n2 != 1 || n3 != 1) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "pending commission submit update failed");
        }
    }

    /** 审核通过/拒绝时核销 pending（须已锁定双方账户行）。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void subtractPendingOnProfitResolved(Long childUserId, Long referrerUserId,
            BigDecimal childPendingOutToReferrer,
            BigDecimal childPendingInRetained) {
        BigDecimal out = MoneyUtil.money(childPendingOutToReferrer);
        BigDecimal childIn = MoneyUtil.money(childPendingInRetained);
        BigDecimal refIn = MoneyUtil.money(childPendingOutToReferrer);
        int n1 = userAccountSummaryMapper.subtractPendingCommissionOut(childUserId, out);
        int n2 = userAccountSummaryMapper.subtractPendingCommissionIn(childUserId, childIn);
        int n3 = userAccountSummaryMapper.subtractPendingCommissionIn(referrerUserId, refIn);
        if (n1 != 1 || n2 != 1 || n3 != 1) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "pending commission release failed");
        }
    }
}

package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.mapper.ProfitDistributionMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.AccountSummaryVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountOverviewService {

    private final ProfitDistributionMapper profitDistributionMapper;
    private final SettlementOrderMapper settlementOrderMapper;

    public AccountSummaryVo summary(Long userId) {
        BigDecimal totalEarned = MoneyUtil.money(profitDistributionMapper.sumIncomeByBeneficiary(userId));

        BigDecimal outApproved = sumFromUser(userId, List.of(SettlementOrderStatus.APPROVED));
        BigDecimal inApproved = sumToUser(userId, List.of(SettlementOrderStatus.APPROVED));

        BigDecimal outPending = sumFromUser(userId, List.of(
                SettlementOrderStatus.PENDING_SUBMIT, SettlementOrderStatus.PENDING_REVIEW));
        BigDecimal inPending = sumToUser(userId, List.of(
                SettlementOrderStatus.PENDING_SUBMIT, SettlementOrderStatus.PENDING_REVIEW));

        return AccountSummaryVo.builder()
                .totalProfitAmount(totalEarned)
                .totalCommissionOutAmount(outApproved)
                .totalCommissionInAmount(inApproved)
                .pendingCommissionOutAmount(outPending)
                .pendingCommissionInAmount(inPending)
                .build();
    }

    private BigDecimal sumFromUser(Long userId, List<SettlementOrderStatus> statuses) {
        List<SettlementOrder> rows = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId)
                .in(SettlementOrder::getStatus, statuses));
        return sumPay(rows);
    }

    private BigDecimal sumToUser(Long userId, List<SettlementOrderStatus> statuses) {
        List<SettlementOrder> rows = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .in(SettlementOrder::getStatus, statuses));
        return sumPay(rows);
    }

    private static BigDecimal sumPay(List<SettlementOrder> rows) {
        BigDecimal acc = MoneyUtil.money(BigDecimal.ZERO);
        for (SettlementOrder r : rows) {
            acc = MoneyUtil.money(acc.add(MoneyUtil.money(r.getPayAmount())));
        }
        return acc;
    }
}

package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgReplenishmentRepayApply;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.enums.ProfitReportStatus;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.enums.RepayStatusEnum;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.BtgReplenishmentRepayApplyMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.ProfitReportMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.service.DashboardService;
import com.btg.commission.service.SettlementOrderService;
import com.btg.commission.vo.PendingSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BtgUserMapper btgUserMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final SettlementOrderService settlementOrderService;
    private final ProfitReportMapper profitReportMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final BtgReplenishmentRepayApplyMapper replenishmentRepayApplyMapper;

    @Override
    public PendingSummaryVO getPendingSummary(Long currentUserId) {
        int settlement = 0;
        int profitReport = 0;
        int settlementPayable = 0;
        int replenishment = 0;
        int repay = 0;

        BtgUser self = btgUserMapper.selectById(currentUserId);
        if (self != null) {
            settlement = toBoundedInt(settlementOrderMapper.selectCount(new LambdaQueryWrapper<SettlementOrder>()
                    .eq(SettlementOrder::getToUserId, currentUserId)
                    .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_REVIEW)));

            profitReport = toBoundedInt(profitReportMapper.selectCount(new LambdaQueryWrapper<ProfitReport>()
                    .eq(ProfitReport::getDirectParentUserId, currentUserId)
                    .eq(ProfitReport::getStatus, ProfitReportStatus.PENDING_DIRECT_REVIEW)));

            settlementPayable = toBoundedInt(settlementOrderService.countMinePayables(currentUserId));

            if (Boolean.TRUE.equals(self.getIsRoot())) {
                replenishment = toBoundedInt(replenishmentApplyMapper.selectCount(new LambdaQueryWrapper<BtgReplenishmentApply>()
                        .in(BtgReplenishmentApply::getStatus,
                                ReplenishmentStatusEnum.PENDING_AUDIT,
                                ReplenishmentStatusEnum.PENDING_SUPPLEMENT,
                                ReplenishmentStatusEnum.PENDING_TRANSFER)));

                repay = toBoundedInt(replenishmentRepayApplyMapper.selectCount(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                        .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.PENDING_AUDIT)));
            }
        }

        int total = settlement + profitReport + settlementPayable + replenishment + repay;
        return PendingSummaryVO.builder()
                .hasPending(total > 0)
                .pendingSettlementReviewCount(settlement)
                .pendingProfitReportReviewCount(profitReport)
                .pendingSettlementPayableCount(settlementPayable)
                .pendingReplenishmentReviewCount(replenishment)
                .pendingReplenishmentRepayReviewCount(repay)
                .totalPendingCount(total)
                .build();
    }

    private static int toBoundedInt(Long count) {
        if (count == null || count <= 0L) {
            return 0;
        }
        if (count >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return count.intValue();
    }
}

package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgReplenishmentRepayApply;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.enums.DashboardTodoType;
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
import com.btg.commission.service.UserService;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.PendingSummaryVO;
import com.btg.commission.vo.flow.DashboardTodoItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BtgUserMapper btgUserMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final SettlementOrderService settlementOrderService;
    private final ProfitReportMapper profitReportMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final BtgReplenishmentRepayApplyMapper replenishmentRepayApplyMapper;
    private final UserService userService;

    @Override
    public PendingSummaryVO getPendingSummary(Long currentUserId) {
        int settlement = 0;
        int profitReport = 0;
        int settlementPayable = 0;
        int replenishment = 0;
        int repay = 0;
        int returnedProfit = 0;
        int returnedReplenishment = 0;
        int returnedRepay = 0;

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
                                ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW,
                                ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL)));
            }

            repay = toBoundedInt(replenishmentRepayApplyMapper.selectCount(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                    .eq(BtgReplenishmentRepayApply::getCapitalUserId, currentUserId)
                    .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.PENDING_CAPITAL_REVIEW)));

            returnedProfit = toBoundedInt(profitReportMapper.selectCount(new LambdaQueryWrapper<ProfitReport>()
                    .eq(ProfitReport::getReportUserId, currentUserId)
                    .eq(ProfitReport::getStatus, ProfitReportStatus.RETURNED_TO_APPLICANT)));

            returnedReplenishment = toBoundedInt(replenishmentApplyMapper.selectCount(new LambdaQueryWrapper<BtgReplenishmentApply>()
                    .eq(BtgReplenishmentApply::getUserId, currentUserId)
                    .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.REJECTED)));

            returnedRepay = toBoundedInt(replenishmentRepayApplyMapper.selectCount(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                    .eq(BtgReplenishmentRepayApply::getUserId, currentUserId)
                    .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.RETURNED_TO_APPLICANT)));
        }

        int total = settlement + profitReport + settlementPayable + replenishment + repay
                + returnedProfit + returnedReplenishment + returnedRepay;
        return PendingSummaryVO.builder()
                .hasPending(total > 0)
                .pendingSettlementReviewCount(settlement)
                .pendingProfitReportReviewCount(profitReport)
                .pendingSettlementPayableCount(settlementPayable)
                .pendingReplenishmentReviewCount(replenishment)
                .pendingReplenishmentRepayReviewCount(repay)
                .returnedProfitReportCount(returnedProfit)
                .returnedReplenishmentApplyCount(returnedReplenishment)
                .returnedReplenishmentRepayCount(returnedRepay)
                .totalPendingCount(total)
                .build();
    }

    @Override
    public List<DashboardTodoItemVO> listTodoItems(Long currentUserId) {
        BtgUser self = btgUserMapper.selectById(currentUserId);
        if (self == null) {
            return List.of();
        }
        List<DashboardTodoItemVO> out = new ArrayList<>();

        for (SettlementOrder o : settlementOrderService.listMinePayables(currentUserId)) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.SETTLEMENT_PAYABLE)
                    .businessId(o.getId())
                    .title("待支付给上级的结算单")
                    .currentStatus(o.getStatus() == null ? null : o.getStatus().name())
                    .currentHandlerUserId(o.getToUserId())
                    .lastRejectReason(null)
                    .latestOperateTime(pickLatestTime(o.getSubmitTime(), o.getUpdatedAt(), o.getCreatedAt()))
                    .routeHint("settlement")
                    .actionHint("请提交转账凭证或等待上级审核")
                    .build());
        }

        for (SettlementOrder o : settlementOrderService.listPendingReviewForMe(currentUserId)) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.SETTLEMENT_REVIEW)
                    .businessId(o.getId())
                    .title("待审核下级结算单")
                    .currentStatus(o.getStatus() == null ? null : o.getStatus().name())
                    .currentHandlerUserId(currentUserId)
                    .lastRejectReason(o.getAuditRemark())
                    .latestOperateTime(pickLatestTime(o.getSubmitTime(), o.getUpdatedAt(), o.getCreatedAt()))
                    .routeHint("settlement-review")
                    .actionHint("审核下级提交的转账凭证")
                    .build());
        }

        List<ProfitReport> pendingProfit = profitReportMapper.selectList(new LambdaQueryWrapper<ProfitReport>()
                .eq(ProfitReport::getDirectParentUserId, currentUserId)
                .eq(ProfitReport::getStatus, ProfitReportStatus.PENDING_DIRECT_REVIEW)
                .orderByDesc(ProfitReport::getSubmitTime)
                .last("LIMIT 50"));
        for (ProfitReport r : pendingProfit) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.PROFIT_REPORT_REVIEW)
                    .businessId(r.getId())
                    .title("待审核下级利润上报 " + (r.getReportNo() != null ? r.getReportNo() : ""))
                    .currentStatus(r.getStatus() == null ? null : r.getStatus().name())
                    .currentHandlerUserId(currentUserId)
                    .lastRejectReason(null)
                    .latestOperateTime(pickLatestTime(r.getSubmitTime(), r.getUpdatedAt(), r.getCreatedAt()))
                    .routeHint("profit-report")
                    .actionHint("审核或退回修改")
                    .build());
        }

        List<Long> descendantIds = userService.listDescendantUserIds(currentUserId);
        if (descendantIds != null && !descendantIds.isEmpty()) {
            List<ProfitReport> chainWatch = profitReportMapper.selectList(new LambdaQueryWrapper<ProfitReport>()
                    .in(ProfitReport::getReportUserId, descendantIds)
                    .in(ProfitReport::getStatus,
                            ProfitReportStatus.PENDING_DIRECT_REVIEW,
                            ProfitReportStatus.IN_SETTLEMENT_CHAIN,
                            ProfitReportStatus.RETURNED_TO_APPLICANT)
                    .orderByDesc(ProfitReport::getSubmitTime)
                    .last("LIMIT 50"));
            for (ProfitReport r : chainWatch) {
                if (!userService.isUpstreamOf(currentUserId, r.getReportUserId())) {
                    continue;
                }
                if (Objects.equals(currentUserId, r.getReportUserId())) {
                    continue;
                }
                if (Objects.equals(currentUserId, r.getDirectParentUserId())
                        && r.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW) {
                    continue;
                }
                if (Objects.equals(currentUserId, r.getDirectParentUserId())
                        && r.getStatus() == ProfitReportStatus.IN_SETTLEMENT_CHAIN) {
                    continue;
                }
                String title = switch (r.getStatus()) {
                    case PENDING_DIRECT_REVIEW -> chainWatchPendingDirectReviewTitle(r);
                    case IN_SETTLEMENT_CHAIN -> "下级利润结算进行中 "
                            + (r.getReportNo() != null ? r.getReportNo() : "");
                    case RETURNED_TO_APPLICANT -> "下级利润上报已退回 "
                            + (r.getReportNo() != null ? r.getReportNo() : "");
                    default -> "下级利润单 " + (r.getReportNo() != null ? r.getReportNo() : "");
                };
                out.add(DashboardTodoItemVO.builder()
                        .todoType(DashboardTodoType.PROFIT_REPORT_CHAIN_WATCH)
                        .businessId(r.getId())
                        .title(title)
                        .currentStatus(r.getStatus() == null ? null : r.getStatus().name())
                        .currentHandlerUserId(r.getCurrentHandlerUserId())
                        .lastRejectReason(r.getLastRejectReason())
                        .latestOperateTime(pickLatestTime(r.getSubmitTime(), r.getUpdatedAt(), r.getCreatedAt()))
                        .routeHint("profit-flow")
                        .actionHint("查看链路（只读）")
                        .build());
            }

            List<BtgReplenishmentApply> rfChainWatch = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                    .in(BtgReplenishmentApply::getUserId, descendantIds)
                    .in(BtgReplenishmentApply::getStatus,
                            ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW,
                            ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL,
                            ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT,
                            ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM,
                            ReplenishmentStatusEnum.RETURNED_TO_CAPITAL)
                    .orderByDesc(BtgReplenishmentApply::getSubmitTime)
                    .last("LIMIT 50"));
            for (BtgReplenishmentApply a : rfChainWatch) {
                if (!userService.isUpstreamOf(currentUserId, a.getUserId())) {
                    continue;
                }
                if (Objects.equals(currentUserId, a.getUserId())) {
                    continue;
                }
                out.add(DashboardTodoItemVO.builder()
                        .todoType(DashboardTodoType.REPLENISHMENT_CHAIN_WATCH)
                        .businessId(a.getId())
                        .title("下级补仓进行中 " + (a.getApplyNo() != null ? a.getApplyNo() : ""))
                        .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                        .currentHandlerUserId(a.getCurrentHandlerUserId())
                        .lastRejectReason(a.getLastRejectReason())
                        .latestOperateTime(pickLatestTime(a.getSubmitTime(), a.getUpdatedAt(), a.getCreatedAt()))
                        .routeHint("replenishment")
                        .actionHint("查看进度（只读）")
                        .build());
            }

            List<BtgReplenishmentRepayApply> repayChainWatch = replenishmentRepayApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                    .in(BtgReplenishmentRepayApply::getUserId, descendantIds)
                    .in(BtgReplenishmentRepayApply::getStatus,
                            RepayStatusEnum.PENDING_CAPITAL_REVIEW,
                            RepayStatusEnum.RETURNED_TO_APPLICANT)
                    .orderByDesc(BtgReplenishmentRepayApply::getSubmitTime)
                    .last("LIMIT 50"));
            for (BtgReplenishmentRepayApply ra : repayChainWatch) {
                if (!userService.isUpstreamOf(currentUserId, ra.getUserId())) {
                    continue;
                }
                if (Objects.equals(currentUserId, ra.getUserId())) {
                    continue;
                }
                String title = ra.getStatus() == RepayStatusEnum.RETURNED_TO_APPLICANT
                        ? "下级归仓已退回待修改 " + (ra.getRepayNo() != null ? ra.getRepayNo() : "")
                        : "下级归仓待资方审核 " + (ra.getRepayNo() != null ? ra.getRepayNo() : "");
                out.add(DashboardTodoItemVO.builder()
                        .todoType(DashboardTodoType.REPLENISHMENT_REPAY_CHAIN_WATCH)
                        .businessId(ra.getId())
                        .title(title)
                        .currentStatus(ra.getStatus() == null ? null : ra.getStatus().name())
                        .currentHandlerUserId(ra.getCurrentHandlerUserId())
                        .lastRejectReason(ra.getLastRejectReason())
                        .latestOperateTime(pickLatestTime(ra.getSubmitTime(), ra.getUpdatedAt(), ra.getCreatedAt()))
                        .routeHint("repay-flow")
                        .actionHint("查看归仓进度（只读）")
                        .build());
            }
        }

        List<ProfitReport> returnedProfit = profitReportMapper.selectList(new LambdaQueryWrapper<ProfitReport>()
                .eq(ProfitReport::getReportUserId, currentUserId)
                .eq(ProfitReport::getStatus, ProfitReportStatus.RETURNED_TO_APPLICANT)
                .orderByDesc(ProfitReport::getUpdatedAt)
                .last("LIMIT 50"));
        for (ProfitReport r : returnedProfit) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.PROFIT_REPORT_RETURNED)
                    .businessId(r.getId())
                    .title("利润上报被退回，请修改后重提 " + (r.getReportNo() != null ? r.getReportNo() : ""))
                    .currentStatus(r.getStatus() == null ? null : r.getStatus().name())
                    .currentHandlerUserId(r.getCurrentHandlerUserId())
                    .lastRejectReason(r.getLastRejectReason())
                    .latestOperateTime(pickLatestTime(r.getLastRejectTime(), r.getUpdatedAt(), r.getSubmitTime()))
                    .routeHint("profit-report")
                    .actionHint("修改金额与凭证后重新提交")
                    .build());
        }

        List<BtgReplenishmentApply> returnedRf = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, currentUserId)
                .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.REJECTED)
                .orderByDesc(BtgReplenishmentApply::getUpdatedAt)
                .last("LIMIT 50"));
        for (BtgReplenishmentApply a : returnedRf) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.REPLENISHMENT_RETURNED)
                    .businessId(a.getId())
                    .title("补仓申请已被管理员拒绝，可修改后重提 " + (a.getApplyNo() != null ? a.getApplyNo() : ""))
                    .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                    .currentHandlerUserId(a.getCurrentHandlerUserId())
                    .lastRejectReason(a.getLastRejectReason())
                    .latestOperateTime(pickLatestTime(a.getLastRejectTime(), a.getUpdatedAt(), a.getSubmitTime()))
                    .routeHint("replenishment")
                    .actionHint("修改余额与截图后重新提交")
                    .build());
        }

        List<BtgReplenishmentApply> applicantConfirmRf = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, currentUserId)
                .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM)
                .orderByDesc(BtgReplenishmentApply::getUpdatedAt)
                .last("LIMIT 50"));
        for (BtgReplenishmentApply a : applicantConfirmRf) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.REPLENISHMENT_APPLICANT_CONFIRM)
                    .businessId(a.getId())
                    .title("请确认补仓到账 " + (a.getApplyNo() != null ? a.getApplyNo() : ""))
                    .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                    .currentHandlerUserId(a.getCurrentHandlerUserId())
                    .lastRejectReason(null)
                    .latestOperateTime(pickLatestTime(a.getCapitalSubmitTime(), a.getUpdatedAt(), a.getSubmitTime()))
                    .routeHint("replenishment")
                    .actionHint("确认到账或拒绝")
                    .build());
        }

        List<BtgReplenishmentApply> capitalRf = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getAssignedCapitalUserId, currentUserId)
                .in(BtgReplenishmentApply::getStatus,
                        ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT,
                        ReplenishmentStatusEnum.RETURNED_TO_CAPITAL)
                .orderByDesc(BtgReplenishmentApply::getUpdatedAt)
                .last("LIMIT 50"));
        for (BtgReplenishmentApply a : capitalRf) {
            String hint = a.getStatus() == ReplenishmentStatusEnum.RETURNED_TO_CAPITAL
                    ? "申请人未确认到账，请更新凭证后提交"
                    : "请上传补仓转账凭证并提交";
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.REPLENISHMENT_CAPITAL_SUBMIT)
                    .businessId(a.getId())
                    .title("待处理补仓单（资方） " + (a.getApplyNo() != null ? a.getApplyNo() : ""))
                    .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                    .currentHandlerUserId(a.getCurrentHandlerUserId())
                    .lastRejectReason(a.getArrivalConfirmRemark())
                    .latestOperateTime(pickLatestTime(a.getCapitalSubmitTime(), a.getUpdatedAt(), a.getSubmitTime()))
                    .routeHint("replenishment")
                    .actionHint(hint)
                    .build());
        }

        List<BtgReplenishmentRepayApply> returnedRepay = replenishmentRepayApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getUserId, currentUserId)
                .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.RETURNED_TO_APPLICANT)
                .orderByDesc(BtgReplenishmentRepayApply::getUpdatedAt)
                .last("LIMIT 50"));
        for (BtgReplenishmentRepayApply a : returnedRepay) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.REPLENISHMENT_REPAY_RETURNED_TO_APPLICANT)
                    .businessId(a.getId())
                    .title("归仓申请被退回，请修改后重提 " + (a.getRepayNo() != null ? a.getRepayNo() : ""))
                    .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                    .currentHandlerUserId(a.getCurrentHandlerUserId())
                    .lastRejectReason(a.getLastRejectReason())
                    .latestOperateTime(pickLatestTime(a.getLastRejectTime(), a.getUpdatedAt(), a.getSubmitTime()))
                    .routeHint("repay")
                    .actionHint("修改归仓金额与截图后重新提交")
                    .build());
        }

        if (Boolean.TRUE.equals(self.getIsRoot())) {
            List<BtgReplenishmentApply> pendAdminRf = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                    .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW)
                    .orderByAsc(BtgReplenishmentApply::getSubmitTime)
                    .last("LIMIT 50"));
            for (BtgReplenishmentApply a : pendAdminRf) {
                out.add(DashboardTodoItemVO.builder()
                        .todoType(DashboardTodoType.REPLENISHMENT_ADMIN_REVIEW)
                        .businessId(a.getId())
                        .title("待管理员审核补仓 " + (a.getApplyNo() != null ? a.getApplyNo() : ""))
                        .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                        .currentHandlerUserId(currentUserId)
                        .lastRejectReason(null)
                        .latestOperateTime(pickLatestTime(a.getSubmitTime(), a.getUpdatedAt(), a.getCreatedAt()))
                        .routeHint("admin-replenishment")
                        .actionHint("审核或拒绝")
                        .build());
            }
            List<BtgReplenishmentApply> pendAssignRf = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                    .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL)
                    .orderByAsc(BtgReplenishmentApply::getSubmitTime)
                    .last("LIMIT 50"));
            for (BtgReplenishmentApply a : pendAssignRf) {
                out.add(DashboardTodoItemVO.builder()
                        .todoType(DashboardTodoType.REPLENISHMENT_ADMIN_REVIEW)
                        .businessId(a.getId())
                        .title("待转派资方执行人 " + (a.getApplyNo() != null ? a.getApplyNo() : ""))
                        .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                        .currentHandlerUserId(currentUserId)
                        .lastRejectReason(null)
                        .latestOperateTime(pickLatestTime(a.getSubmitTime(), a.getUpdatedAt(), a.getCreatedAt()))
                        .routeHint("admin-replenishment")
                        .actionHint("转派资方处理")
                        .build());
            }
        }

        List<BtgReplenishmentRepayApply> pendRepayCapital = replenishmentRepayApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getCapitalUserId, currentUserId)
                .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.PENDING_CAPITAL_REVIEW)
                .orderByAsc(BtgReplenishmentRepayApply::getSubmitTime)
                .last("LIMIT 50"));
        for (BtgReplenishmentRepayApply a : pendRepayCapital) {
            out.add(DashboardTodoItemVO.builder()
                    .todoType(DashboardTodoType.REPLENISHMENT_REPAY_CAPITAL_REVIEW)
                    .businessId(a.getId())
                    .title("待审核归仓申请 " + (a.getRepayNo() != null ? a.getRepayNo() : ""))
                    .currentStatus(a.getStatus() == null ? null : a.getStatus().name())
                    .currentHandlerUserId(a.getCurrentHandlerUserId())
                    .lastRejectReason(null)
                    .latestOperateTime(pickLatestTime(a.getSubmitTime(), a.getUpdatedAt(), a.getCreatedAt()))
                    .routeHint("repay")
                    .actionHint("审核或退回申请人修改")
                    .build());
        }

        out.sort(Comparator.comparing(DashboardTodoItemVO::getLatestOperateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    private String chainWatchPendingDirectReviewTitle(ProfitReport r) {
        BtgUser reporter = r.getReportUserId() == null ? null : btgUserMapper.selectById(r.getReportUserId());
        String who = reporterDisplayLabel(reporter, r.getReportUserId());
        BigDecimal amt = r.getProfitAmount();
        String amtStr = amt == null ? "—" : MoneyUtil.money(amt).stripTrailingZeros().toPlainString();
        return who + " 上报了 " + amtStr + " 元的利润单待其直属上级审核";
    }

    private static String reporterDisplayLabel(BtgUser u, Long userId) {
        if (u != null) {
            if (StringUtils.hasText(u.getNickname())) {
                return u.getNickname().trim();
            }
            if (StringUtils.hasText(u.getMobile())) {
                return u.getMobile().trim();
            }
        }
        return userId == null ? "用户" : ("用户" + userId);
    }

    private static LocalDateTime pickLatestTime(LocalDateTime a, LocalDateTime b, LocalDateTime c) {
        LocalDateTime best = null;
        for (LocalDateTime x : new LocalDateTime[]{a, b, c}) {
            if (x == null) {
                continue;
            }
            if (best == null || x.isAfter(best)) {
                best = x;
            }
        }
        return best;
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

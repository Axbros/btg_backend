package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.TodoReminderReconcileLog;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.TodoReminderReconcileLogMapper;
import com.btg.commission.service.impl.DashboardServiceImpl;
import com.btg.commission.vo.PendingSummaryVO;
import com.btg.commission.vo.ReminderReconcileItemVO;
import com.btg.commission.vo.ReminderReconcileResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReminderReconcileService {

    private final DashboardServiceImpl dashboardService;
    private final BtgUserMapper btgUserMapper;
    private final TodoReminderReconcileLogMapper reconcileLogMapper;

    public ReminderReconcileResultVO reconcilePendingSummaryForUser(Long userId, boolean persistLog) {
        PendingSummaryVO legacy = dashboardService.buildLegacyPendingSummary(userId);
        PendingSummaryVO reminder = dashboardService.buildReminderPendingSummary(userId);
        LocalDateTime now = LocalDateTime.now();
        List<ReminderReconcileItemVO> items = new ArrayList<>(PendingSummaryMetric.values().length);
        for (PendingSummaryMetric metric : PendingSummaryMetric.values()) {
            items.add(item(metric.metricKey, metric.read(legacy), metric.read(reminder)));
        }
        boolean hasDiff = items.stream().anyMatch(i -> i.getDiffCount() != null && i.getDiffCount() != 0);
        if (persistLog) {
            for (ReminderReconcileItemVO i : items) {
                TodoReminderReconcileLog row = new TodoReminderReconcileLog();
                row.setUserId(userId);
                row.setMetricKey(i.getMetricKey());
                row.setLegacyCount(i.getLegacyCount());
                row.setReminderCount(i.getReminderCount());
                row.setDiffCount(i.getDiffCount());
                row.setComparedAt(now);
                reconcileLogMapper.insert(row);
            }
        }
        return ReminderReconcileResultVO.builder()
                .userId(userId)
                .hasDiff(hasDiff)
                .comparedAt(now)
                .items(items)
                .build();
    }

    public List<ReminderReconcileResultVO> reconcileLatestUsers(int size, boolean persistLog) {
        int n = Math.max(1, Math.min(size, 100));
        List<BtgUser> users = btgUserMapper.selectList(new LambdaQueryWrapper<BtgUser>()
                .select(BtgUser::getId)
                .orderByDesc(BtgUser::getId)
                .last("LIMIT " + n));
        List<ReminderReconcileResultVO> out = new ArrayList<>(users.size());
        for (BtgUser u : users) {
            if (u.getId() == null) {
                continue;
            }
            out.add(reconcilePendingSummaryForUser(u.getId(), persistLog));
        }
        return out;
    }

    private static ReminderReconcileItemVO item(String metricKey, Integer legacyCount, Integer reminderCount) {
        int l = legacyCount == null ? 0 : legacyCount;
        int r = reminderCount == null ? 0 : reminderCount;
        return ReminderReconcileItemVO.builder()
                .metricKey(metricKey)
                .legacyCount(l)
                .reminderCount(r)
                .diffCount(r - l)
                .build();
    }

    private enum PendingSummaryMetric {
        PENDING_SETTLEMENT_REVIEW("pendingSettlementReviewCount", PendingSummaryVO::getPendingSettlementReviewCount),
        PENDING_PROFIT_REPORT_REVIEW("pendingProfitReportReviewCount", PendingSummaryVO::getPendingProfitReportReviewCount),
        PENDING_SETTLEMENT_PAYABLE("pendingSettlementPayableCount", PendingSummaryVO::getPendingSettlementPayableCount),
        PENDING_REPLENISHMENT_REVIEW("pendingReplenishmentReviewCount", PendingSummaryVO::getPendingReplenishmentReviewCount),
        PENDING_QUALIFICATION_REVIEW("pendingQualificationReviewCount", PendingSummaryVO::getPendingQualificationReviewCount),
        PENDING_PROFIT_CONFIG_MODE_AUDIT("pendingProfitConfigModeAuditCount", PendingSummaryVO::getPendingProfitConfigModeAuditCount),
        PENDING_REPLENISHMENT_REPAY_REVIEW("pendingReplenishmentRepayReviewCount", PendingSummaryVO::getPendingReplenishmentRepayReviewCount),
        PENDING_REPLENISHMENT_APPLICANT_CONFIRM("pendingReplenishmentApplicantConfirmCount", PendingSummaryVO::getPendingReplenishmentApplicantConfirmCount),
        RETURNED_PROFIT_REPORT("returnedProfitReportCount", PendingSummaryVO::getReturnedProfitReportCount),
        RETURNED_REPLENISHMENT_APPLY("returnedReplenishmentApplyCount", PendingSummaryVO::getReturnedReplenishmentApplyCount),
        RETURNED_REPLENISHMENT_REPAY("returnedReplenishmentRepayCount", PendingSummaryVO::getReturnedReplenishmentRepayCount),
        TOTAL_PENDING("totalPendingCount", PendingSummaryVO::getTotalPendingCount);

        private final String metricKey;
        private final Function<PendingSummaryVO, Integer> extractor;

        PendingSummaryMetric(String metricKey, Function<PendingSummaryVO, Integer> extractor) {
            this.metricKey = metricKey;
            this.extractor = extractor;
        }

        private Integer read(PendingSummaryVO summary) {
            return extractor.apply(summary);
        }
    }
}

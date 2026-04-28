package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.ProfitDistribution;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfitConfig;
import com.btg.commission.enums.CommissionModeEnum;
import com.btg.commission.enums.ReminderTodoTypeEnum;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.enums.UserProfitConfigStatus;
import com.btg.commission.mapper.ProfitDistributionMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfitConfigMapper;
import com.btg.commission.util.MoneyUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 按祖先链解析分润比例、校验单调性，并落库分润明细与逐级结算单（首笔待审核，其余 INIT）。
 */
@Service
@RequiredArgsConstructor
public class ProfitDistributionService {

    private final BtgUserMapper btgUserMapper;
    private final UserProfitConfigMapper userProfitConfigMapper;
    private final ProfitDistributionMapper profitDistributionMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final TodoReminderService todoReminderService;

    @Getter
    @RequiredArgsConstructor
    public static final class BuiltChain {
        private final List<Long> userIdsRootToLeaf;
        private final List<BigDecimal> edgeRatios;
    }

    /**
     * @param mode 整条链统一使用的分润模式快照，须与 {@code btg_profit_report.commission_mode} 一致（创建/重提时写入），
     *             不得用直属上级配置的最新值覆盖历史利润单。
     */
    public BuiltChain buildChainOrThrow(Long reportUserId, CommissionModeEnum mode) {
        if (mode == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "分润模式不能为空");
        }
        BtgUser reporter = btgUserMapper.selectById(reportUserId);
        if (reporter == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        List<Long> chain = parseChainFromRootToUser(reporter);
        if (chain.size() < 2) {
            throw new BizException(ResultCode.CONFLICT, "根用户不能提交利润上报");
        }
        List<BigDecimal> ratios = new ArrayList<>();
        for (int i = 1; i < chain.size(); i++) {
            Long parentId = chain.get(i - 1);
            Long childId = chain.get(i);
            UserProfitConfig cfg = userProfitConfigMapper.selectOne(new LambdaQueryWrapper<UserProfitConfig>()
                    .eq(UserProfitConfig::getParentUserId, parentId)
                    .eq(UserProfitConfig::getChildUserId, childId)
                    .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                    .last("LIMIT 1"));
            if (cfg == null) {
                throw new BizException(ResultCode.CONFLICT, "链路未配置分润比例：" + parentId + " -> " + childId);
            }
            ratios.add(resolveRatioByMode(cfg, mode));
        }
        validateMonotoneRatios(ratios);
        return new BuiltChain(chain, ratios);
    }

    private static BigDecimal rawRatioByMode(UserProfitConfig cfg, CommissionModeEnum mode) {
        if (cfg == null || mode == null) {
            return null;
        }
        if (mode == CommissionModeEnum.NON_GUARANTEE) {
            return cfg.getNonGuaranteeRatio() != null ? cfg.getNonGuaranteeRatio() : cfg.getChildProfitRatio();
        }
        return cfg.getGuaranteeRatio() != null ? cfg.getGuaranteeRatio() : cfg.getChildProfitRatio();
    }

    /**
     * 按快照模式取该边上「子级线」相对总利润的比例（已刻度归一）。
     * 优先 {@code guarantee_ratio}/{@code non_guarantee_ratio}，兼容旧数据回落 {@code child_profit_ratio}；均无则抛错。
     */
    public static BigDecimal resolveRatioByMode(UserProfitConfig config, CommissionModeEnum mode) {
        BigDecimal raw = rawRatioByMode(config, mode);
        if (raw == null) {
            throw new BizException(ResultCode.CONFLICT,
                    "链路未配置有效分润比例：" + config.getParentUserId() + " -> " + config.getChildUserId());
        }
        return MoneyUtil.profitRatio(raw);
    }

    /** 详情展示等：无有效比例时返回 null，不中断请求 */
    public static BigDecimal resolveRatioByModeOrNull(UserProfitConfig config, CommissionModeEnum mode) {
        BigDecimal raw = rawRatioByMode(config, mode);
        return raw == null ? null : MoneyUtil.profitRatio(raw);
    }

    /**
     * 重提前清理旧分润与结算单（逻辑删除，保留历史主单 id）。
     */
    public void softDeleteDistributionsAndSettlementsByReportId(Long reportId) {
        profitDistributionMapper.delete(new LambdaQueryWrapper<ProfitDistribution>()
                .eq(ProfitDistribution::getReportId, reportId));
        settlementOrderMapper.delete(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, reportId));
    }

    public void persistDistributionsAndSettlements(
            Long reportId,
            BigDecimal profitAmount,
            BuiltChain chain,
            String bottomTransferScreenshotUrl,
            String commissionModeSnapshot) {
        List<Long> users = chain.getUserIdsRootToLeaf();
        List<BigDecimal> r = chain.getEdgeRatios();
        int n = users.size() - 1;
        BigDecimal p = MoneyUtil.money(profitAmount);

        for (int j = 0; j <= n; j++) {
            ProfitDistribution row = new ProfitDistribution();
            row.setReportId(reportId);
            row.setCommissionMode(commissionModeSnapshot);
            row.setBeneficiaryUserId(users.get(j));
            row.setLevelNo(j);
            BigDecimal upper;
            BigDecimal lower;
            if (j == 0) {
                upper = BigDecimal.ONE;
                lower = r.get(0);
            } else if (j == n) {
                upper = r.get(n - 1);
                lower = BigDecimal.ZERO;
            } else {
                upper = r.get(j - 1);
                lower = r.get(j);
            }
            row.setUpperRatio(MoneyUtil.profitRatio(upper));
            row.setLowerRatio(MoneyUtil.profitRatio(lower));
            row.setIncomeAmount(MoneyUtil.sliceIncome(p, upper, lower));
            profitDistributionMapper.insert(row);
        }

        for (int k = n; k >= 1; k--) {
            SettlementOrder o = new SettlementOrder();
            o.setRootReportId(reportId);
            o.setFromUserId(users.get(k));
            o.setToUserId(users.get(k - 1));
            o.setLevelNo(n - k);
            BigDecimal edgeRatio = r.get(k - 1);
            o.setPayAmount(MoneyUtil.payToParentAmount(p, edgeRatio));
            if (k == n) {
                o.setStatus(SettlementOrderStatus.PENDING_REVIEW);
                o.setTransferScreenshotUrl(bottomTransferScreenshotUrl);
                o.setSubmitTime(java.time.LocalDateTime.now());
            } else {
                o.setStatus(SettlementOrderStatus.INIT);
            }
            settlementOrderMapper.insert(o);
            if (o.getStatus() == SettlementOrderStatus.PENDING_REVIEW) {
                todoReminderService.upsertOpen(
                        ReminderTodoTypeEnum.SETTLEMENT_REVIEW,
                        "settlement",
                        o.getId(),
                        o.getToUserId(),
                        o.getStatus().name(),
                        o.getUpdatedAt());
            }
        }
    }

    private static void validateMonotoneRatios(List<BigDecimal> ratios) {
        BigDecimal prev = BigDecimal.ONE;
        for (BigDecimal x : ratios) {
            if (x.compareTo(prev) > 0) {
                throw new BizException(ResultCode.CONFLICT, "分润比例必须沿链路单调不增：子级比例不能大于父级当前可分比例");
            }
            if (x.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(ResultCode.CONFLICT, "分润比例不能为负");
            }
            prev = x;
        }
    }

    private static List<Long> parseChainFromRootToUser(BtgUser user) {
        List<Long> ids = new ArrayList<>();
        String path = user.getAncestorPath() == null ? "/" : user.getAncestorPath();
        for (String seg : path.split("/")) {
            if (seg == null || seg.isEmpty()) {
                continue;
            }
            ids.add(Long.parseLong(seg));
        }
        ids.add(user.getId());
        return ids;
    }
}

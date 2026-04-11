package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.ProfitDistribution;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfitConfig;
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

    @Getter
    @RequiredArgsConstructor
    public static final class BuiltChain {
        private final List<Long> userIdsRootToLeaf;
        private final List<BigDecimal> edgeRatios;
    }

    public BuiltChain buildChainOrThrow(Long reportUserId) {
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
            ratios.add(MoneyUtil.profitRatio(cfg.getChildProfitRatio()));
        }
        validateMonotoneRatios(ratios);
        return new BuiltChain(chain, ratios);
    }

    public void persistDistributionsAndSettlements(
            Long reportId,
            BigDecimal profitAmount,
            BuiltChain chain,
            String bottomTransferScreenshotUrl) {
        List<Long> users = chain.getUserIdsRootToLeaf();
        List<BigDecimal> r = chain.getEdgeRatios();
        int n = users.size() - 1;
        BigDecimal p = MoneyUtil.money(profitAmount);

        for (int j = 0; j <= n; j++) {
            ProfitDistribution row = new ProfitDistribution();
            row.setReportId(reportId);
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

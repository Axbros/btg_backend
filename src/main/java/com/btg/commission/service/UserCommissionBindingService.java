package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.binding.BindingCreateRequest;
import com.btg.commission.entity.CommissionStrategy;
import com.btg.commission.entity.SysUser;
import com.btg.commission.entity.UserCommissionBinding;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.BindingStatus;
import com.btg.commission.enums.KycStatus;
import com.btg.commission.enums.StrategyStatus;
import com.btg.commission.mapper.CommissionStrategyMapper;
import com.btg.commission.mapper.SysUserMapper;
import com.btg.commission.mapper.UserCommissionBindingMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.MyActiveCommissionStrategyVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCommissionBindingService {

    private final UserCommissionBindingMapper userCommissionBindingMapper;
    private final SysUserMapper sysUserMapper;
    private final CommissionStrategyMapper commissionStrategyMapper;
    private final UserProfileMapper userProfileMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long bind(Long referrerUserId, BindingCreateRequest req) {
        SysUser child = sysUserMapper.selectById(req.getChildUserId());
        if (child == null) {
            throw new BizException(ResultCode.NOT_FOUND, "child user not found");
        }
        if (!referrerUserId.equals(child.getReferrerUserId())) {
            throw new BizException(ResultCode.CONFLICT, "not your direct referral");
        }
        UserProfile childProfile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, req.getChildUserId())
                .last("LIMIT 1"));
        if (childProfile == null || childProfile.getKycStatus() != KycStatus.APPROVED) {
            throw new BizException(ResultCode.CONFLICT, "下级实名/KYC 未通过审核，无法绑定分佣策略");
        }
        CommissionStrategy strategy = commissionStrategyMapper.selectById(req.getStrategyId());
        if (strategy == null || strategy.getStatus() != StrategyStatus.ENABLED) {
            throw new BizException(ResultCode.CONFLICT, "strategy invalid or disabled");
        }

        UserCommissionBinding active = userCommissionBindingMapper.selectOne(new LambdaQueryWrapper<UserCommissionBinding>()
                .eq(UserCommissionBinding::getReferrerUserId, referrerUserId)
                .eq(UserCommissionBinding::getChildUserId, req.getChildUserId())
                .eq(UserCommissionBinding::getStatus, BindingStatus.ACTIVE)
                .last("LIMIT 1"));
        if (active != null) {
            active.setStrategyId(strategy.getId());
            active.setCommissionRateSnapshot(MoneyUtil.rate(strategy.getCommissionRate()));
            active.setEffectiveTime(LocalDateTime.now());
            userCommissionBindingMapper.updateById(active);
            return active.getId();
        }

        UserCommissionBinding neu = new UserCommissionBinding();
        neu.setReferrerUserId(referrerUserId);
        neu.setChildUserId(req.getChildUserId());
        neu.setStrategyId(strategy.getId());
        neu.setCommissionRateSnapshot(MoneyUtil.rate(strategy.getCommissionRate()));
        neu.setStatus(BindingStatus.ACTIVE);
        neu.setEffectiveTime(LocalDateTime.now());
        neu.setExpireTime(null);
        userCommissionBindingMapper.insert(neu);
        return neu.getId();
    }

    public UserCommissionBinding findActiveBinding(Long referrerUserId, Long childUserId) {
        return userCommissionBindingMapper.selectOne(new LambdaQueryWrapper<UserCommissionBinding>()
                .eq(UserCommissionBinding::getReferrerUserId, referrerUserId)
                .eq(UserCommissionBinding::getChildUserId, childUserId)
                .eq(UserCommissionBinding::getStatus, BindingStatus.ACTIVE)
                .last("LIMIT 1"));
    }

    /**
     * 下级查看自己当前生效的分佣策略快照，用于收益申报前计算转账截图金额等。
     *
     * @param profitAmountForPreview 可选；传入时返回与 {@link com.btg.commission.service.ProfitRecordService#submit} 一致的预览佣金与应转金额
     */
    public MyActiveCommissionStrategyVo getMyActiveCommissionStrategy(Long userId, BigDecimal profitAmountForPreview) {
        SysUser self = sysUserMapper.selectById(userId);
        if (self == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        Long refId = self.getReferrerUserId();
        if (refId == null || refId == 0) {
            throw new BizException(ResultCode.CONFLICT, "根用户或无推荐人，不适用分佣策略");
        }
        UserCommissionBinding binding = findActiveBinding(refId, userId);
        if (binding == null) {
            throw new BizException(ResultCode.CONFLICT, "直属推荐人尚未为您绑定分佣策略");
        }
        BigDecimal rate = MoneyUtil.rate(binding.getCommissionRateSnapshot());
        BigDecimal transferRatio = MoneyUtil.rate(BigDecimal.ONE.subtract(rate));

        String strategyName = null;
        String strategyCode = null;
        CommissionStrategy st = commissionStrategyMapper.selectById(binding.getStrategyId());
        if (st != null) {
            strategyName = st.getStrategyName();
            strategyCode = st.getStrategyCode();
        }

        MyActiveCommissionStrategyVo.MyActiveCommissionStrategyVoBuilder b = MyActiveCommissionStrategyVo.builder()
                .strategyId(binding.getStrategyId())
                .strategyName(strategyName)
                .strategyCode(strategyCode)
                .commissionRate(rate)
                .transferRatio(transferRatio);

        if (profitAmountForPreview != null) {
            BigDecimal profit = MoneyUtil.money(profitAmountForPreview);
            BigDecimal toReferrer = MoneyUtil.dueShareAmount(profit, rate);
            BigDecimal childRetained = MoneyUtil.commissionShareOfProfit(profit, rate);
            b.previewProfitAmount(profit)
                    .previewCommissionAmount(toReferrer)
                    .previewTransferAmount(toReferrer)
                    .previewNetAmount(childRetained);
        }

        return b.build();
    }
}

package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.entity.CommissionStrategy;
import com.btg.commission.entity.SysUser;
import com.btg.commission.entity.UserCommissionBinding;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.BindingStatus;
import com.btg.commission.enums.KycStatus;
import com.btg.commission.mapper.CommissionStrategyMapper;
import com.btg.commission.mapper.SysUserMapper;
import com.btg.commission.mapper.UserCommissionBindingMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.util.AncestorPathUtil;
import com.btg.commission.vo.PageVo;
import com.btg.commission.vo.TeamMemberBriefVo;
import com.btg.commission.vo.UserDetailVo;
import com.btg.commission.vo.UserMeVo;
import com.btg.commission.vo.UserProfileFullVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCommissionBindingMapper userCommissionBindingMapper;
    private final CommissionStrategyMapper commissionStrategyMapper;

    public UserMeVo me(Long userId) {
        SysUser u = sysUserMapper.selectById(userId);
        if (u == null) {
            return null;
        }
        String referrerNickname = null;
        Long refId = u.getReferrerUserId();
        if (refId != null && refId != 0L) {
            SysUser ref = sysUserMapper.selectById(refId);
            if (ref != null) {
                referrerNickname = ref.getNickname();
            }
        }

        KycStatus kycStatus = null;
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile != null) {
            kycStatus = profile.getKycStatus();
        }

        return UserMeVo.builder()
                .id(u.getId())
                .mobile(u.getMobile())
                .status(u.getStatus())
                .isRoot(u.getIsRoot())
                .referrerUserId(u.getReferrerUserId())
                .ancestorPath(u.getAncestorPath())
                .invitationCode(u.getInvitationCode())
                .nickname(u.getNickname())
                .referrerNickname(referrerNickname)
                .kycStatus(kycStatus)
                .build();
    }

    public PageVo<TeamMemberBriefVo> pageDirectChildren(Long referrerUserId, long page, long pageSize) {
        long p = Math.max(1L, page);
        long s = Math.min(MAX_PAGE_SIZE, Math.max(1L, pageSize));
        Page<TeamMemberBriefVo> mp = new Page<>(p, s);
        Page<TeamMemberBriefVo> result = sysUserMapper.selectDirectChildrenPage(mp, referrerUserId);
        return toPageVo(result);
    }

    public PageVo<TeamMemberBriefVo> pageAllDescendants(Long userId, long page, long pageSize) {
        long p = Math.max(1L, page);
        long s = Math.min(MAX_PAGE_SIZE, Math.max(1L, pageSize));
        SysUser self = sysUserMapper.selectById(userId);
        if (self == null) {
            return PageVo.<TeamMemberBriefVo>builder()
                    .records(Collections.emptyList())
                    .total(0)
                    .page(p)
                    .pageSize(s)
                    .build();
        }
        String prefix = AncestorPathUtil.descendantPathPrefix(self);
        Page<TeamMemberBriefVo> mp = new Page<>(p, s);
        Page<TeamMemberBriefVo> result = sysUserMapper.selectAllDescendantsPage(mp, prefix);
        return toPageVo(result);
    }

    public long countDirectChildren(Long referrerUserId) {
        return sysUserMapper.countDirectChildren(referrerUserId);
    }

    public long countAllDescendants(Long userId) {
        SysUser self = sysUserMapper.selectById(userId);
        if (self == null) {
            return 0;
        }
        return sysUserMapper.countAllDescendants(AncestorPathUtil.descendantPathPrefix(self));
    }

    /**
     * 是否为 target 的直属上级或任意上级（根据 {@code ancestor_path} 与 {@code referrer_user_id} 判断）。
     */
    public boolean isUpstreamOf(Long upstreamUserId, Long targetUserId) {
        if (upstreamUserId == null || targetUserId == null) {
            return false;
        }
        if (upstreamUserId.equals(targetUserId)) {
            return false;
        }
        SysUser target = sysUserMapper.selectById(targetUserId);
        if (target == null) {
            return false;
        }
        if (upstreamUserId.equals(target.getReferrerUserId())) {
            return true;
        }
        String path = target.getAncestorPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String seg : path.split("/")) {
            if (seg.isEmpty()) {
                continue;
            }
            try {
                if (Long.parseLong(seg) == upstreamUserId) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    public UserMeVo getById(Long userId) {
        return me(userId);
    }

    public UserDetailVo getDetailById(Long userId) {
        UserMeVo user = me(userId);
        if (user == null) {
            return null;
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));

        Long strategyId = null;
        String strategyName = null;
        BigDecimal commissionRate = null;
        Long refId = user.getReferrerUserId();
        if (refId != null && refId != 0L) {
            UserCommissionBinding binding = userCommissionBindingMapper.selectOne(new LambdaQueryWrapper<UserCommissionBinding>()
                    .eq(UserCommissionBinding::getChildUserId, userId)
                    .eq(UserCommissionBinding::getReferrerUserId, refId)
                    .eq(UserCommissionBinding::getStatus, BindingStatus.ACTIVE)
                    .last("LIMIT 1"));
            if (binding != null) {
                strategyId = binding.getStrategyId();
                commissionRate = binding.getCommissionRateSnapshot();
                if (strategyId != null) {
                    CommissionStrategy st = commissionStrategyMapper.selectById(strategyId);
                    if (st != null) {
                        strategyName = st.getStrategyName();
                    }
                }
            }
        }

        return UserDetailVo.builder()
                .user(user)
                .profile(profile == null ? null : toProfileFullVo(profile))
//                .strategyId(strategyId)
                .strategyName(strategyName)
                .commissionRate(commissionRate)
                .build();
    }

    private PageVo<TeamMemberBriefVo> toPageVo(Page<TeamMemberBriefVo> result) {
        return PageVo.<TeamMemberBriefVo>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(result.getCurrent())
                .pageSize(result.getSize())
                .build();
    }

    private UserProfileFullVo toProfileFullVo(UserProfile p) {
        return UserProfileFullVo.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .realName(p.getRealName())
                .idCardNo(p.getIdCardNo())
                .idCardFrontUrl(p.getIdCardFrontUrl())
                .idCardBackUrl(p.getIdCardBackUrl())
                .facePhotoUrl(p.getFacePhotoUrl())
                .kycStatus(p.getKycStatus())
                .kycAuditTime(p.getKycAuditTime())
                .kycAuditRemark(p.getKycAuditRemark())
                .serverName(p.getServerName())
                .tradingAccountId(p.getTradingAccountId())
                .tradingAccountPassword(p.getTradingAccountPassword())
                .exchangeUid(p.getExchangeUid())
                .principalAmount(p.getPrincipalAmount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

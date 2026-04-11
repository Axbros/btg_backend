package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.profile.ProfileCompleteRequest;
import com.btg.commission.entity.SysUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.KycStatus;
import com.btg.commission.mapper.SysUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.UserProfileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;

    @Transactional(rollbackFor = Exception.class)
    public UserProfileVo completeProfile(Long userId, ProfileCompleteRequest req) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }

        SysUser userPatch = new SysUser();
        userPatch.setId(userId);
        userPatch.setNickname(req.getNickname().trim());
        sysUserMapper.updateById(userPatch);

        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setKycStatus(KycStatus.NOT_SUBMITTED);
            profile.setPrincipalAmount(MoneyUtil.money(BigDecimal.ZERO));
            userProfileMapper.insert(profile);
        }

        profile.setRealName(req.getRealName().trim());
        if (StringUtils.hasText(req.getIdCardNo())) {
            profile.setIdCardNo(req.getIdCardNo().trim());
        }
        if (req.getIdCardFrontUrl() != null) {
            profile.setIdCardFrontUrl(StringUtils.hasText(req.getIdCardFrontUrl())
                    ? req.getIdCardFrontUrl().trim() : null);
        }
        if (req.getIdCardBackUrl() != null) {
            profile.setIdCardBackUrl(StringUtils.hasText(req.getIdCardBackUrl())
                    ? req.getIdCardBackUrl().trim() : null);
        }
        if (req.getFacePhotoUrl() != null) {
            profile.setFacePhotoUrl(StringUtils.hasText(req.getFacePhotoUrl())
                    ? req.getFacePhotoUrl().trim() : null);
        }
        profile.setServerName(req.getServerName().trim());
        profile.setTradingAccountId(req.getTradingAccountId().trim());
        profile.setTradingAccountPassword(req.getTradingAccountPassword());
        profile.setExchangeUid(req.getExchangeUid().trim());
        profile.setPrincipalAmount(MoneyUtil.money(req.getPrincipalAmount()));

        KycStatus current = profile.getKycStatus() == null ? KycStatus.NOT_SUBMITTED : profile.getKycStatus();
        if (current != KycStatus.APPROVED) {
            profile.setKycStatus(KycStatus.PENDING);
        }

        userProfileMapper.updateById(profile);

        user.setNickname(req.getNickname().trim());
        return toVo(user, profile);
    }

    private UserProfileVo toVo(SysUser user, UserProfile profile) {
        return UserProfileVo.builder()
                .nickname(user.getNickname())
                .realName(profile.getRealName())
                .idCardNo(profile.getIdCardNo())
                .idCardFrontUrl(profile.getIdCardFrontUrl())
                .idCardBackUrl(profile.getIdCardBackUrl())
                .facePhotoUrl(profile.getFacePhotoUrl())
                .kycStatus(profile.getKycStatus())
                .serverName(profile.getServerName())
                .tradingAccountId(profile.getTradingAccountId())
                .exchangeUid(profile.getExchangeUid())
                .principalAmount(profile.getPrincipalAmount())
                .build();
    }
}

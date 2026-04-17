package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.profile.ProfileCompleteRequest;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.QualificationStatusEnum;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.BtgUserMapper;
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

    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;

    public UserProfileVo getProfile(Long userId) {
        BtgUser user = btgUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        return toVo(user, profile);
    }

    public UserProfileVo buildProfileVo(BtgUser user) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, user.getId())
                .last("LIMIT 1"));
        return toVo(user, profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserProfileVo completeProfile(Long userId, ProfileCompleteRequest req) {
        BtgUser user = btgUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(req.getMobile())) {
            String in = req.getMobile().trim();
            String stored = user.getMobile() != null ? user.getMobile().trim() : "";
            if (!in.equals(stored)) {
                throw new BizException(ResultCode.FORBIDDEN, "不允许修改手机号");
            }
        }

        BtgUser userPatch = new BtgUser();
        userPatch.setId(userId);
        userPatch.setNickname(req.getNickname().trim());
        if (user.getStatus() != UserStatus.NORMAL) {
            userPatch.setStatus(UserStatus.PENDING_APPROVAL);
        }
        btgUserMapper.updateById(userPatch);

        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setPrincipalAmount(MoneyUtil.money(BigDecimal.ZERO));
            profile.setQualificationStatus(QualificationStatusEnum.PENDING);
            profile.setQualificationSubmitCount(1);
            userProfileMapper.insert(profile);
        }

        if (StringUtils.hasText(req.getRealName())) {
            profile.setRealName(req.getRealName().trim());
        } else {
            profile.setRealName(null);
        }
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
        if (StringUtils.hasText(req.getTradingAccountPassword())) {
            profile.setTradingAccountPassword(req.getTradingAccountPassword().trim());
        } else if (!StringUtils.hasText(profile.getTradingAccountPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "账户密码不能为空");
        }
        profile.setExchangeUid(req.getExchangeUid().trim());
        profile.setWalletName(req.getWalletName().trim());
        profile.setWalletAddress(req.getWalletAddress().trim());
        profile.setPrincipalAmount(MoneyUtil.money(req.getPrincipalAmount()));

        userProfileMapper.updateById(profile);

        user.setNickname(req.getNickname().trim());
        if (user.getStatus() != UserStatus.NORMAL) {
            user.setStatus(UserStatus.PENDING_APPROVAL);
        }
        return toVo(user, profile);
    }

    private UserProfileVo toVo(BtgUser user, UserProfile profile) {
        UserProfileVo.UserProfileVoBuilder b = UserProfileVo.builder()
                .nickname(user.getNickname());
        if (profile != null) {
            b.realName(profile.getRealName())
                    .idCardNo(profile.getIdCardNo())
                    .idCardFrontUrl(profile.getIdCardFrontUrl())
                    .idCardBackUrl(profile.getIdCardBackUrl())
                    .facePhotoUrl(profile.getFacePhotoUrl())
                    .serverName(profile.getServerName())
                    .tradingAccountId(profile.getTradingAccountId())
                    .exchangeUid(profile.getExchangeUid())
                    .walletName(profile.getWalletName())
                    .walletAddress(profile.getWalletAddress())
                    .principalAmount(profile.getPrincipalAmount())
                    .qualificationStatus(profile.getQualificationStatus() != null
                            ? profile.getQualificationStatus()
                            : QualificationStatusEnum.PENDING)
                    .qualificationAuditTime(profile.getQualificationAuditTime())
                    .qualificationAuditRemark(profile.getQualificationAuditRemark())
                    .qualificationSubmitCount(profile.getQualificationSubmitCount() != null
                            ? profile.getQualificationSubmitCount()
                            : 1)
                    .qualificationLastSubmitTime(profile.getQualificationLastSubmitTime())
                    .canResubmitQualification(profile.getQualificationStatus() == QualificationStatusEnum.REJECTED);
        } else {
            b.canResubmitQualification(Boolean.FALSE);
        }
        return b.build();
    }
}

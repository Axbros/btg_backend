package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.QualificationStatusEnum;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.PendingQualificationUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserQualificationServiceImpl implements UserQualificationService {

    private final UserProfileMapper userProfileMapper;
    private final BtgUserMapper btgUserMapper;
    private final AuditLogService auditLogService;

    @Override
    public Page<PendingQualificationUserVO> pagePendingQualification(long page, long size) {
        Page<UserProfile> p = new Page<>(page, size);
        LambdaQueryWrapper<UserProfile> w = new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getQualificationStatus, QualificationStatusEnum.PENDING)
                .orderByDesc(UserProfile::getCreatedAt);
        Page<UserProfile> raw = userProfileMapper.selectPage(p, w);
        if (raw.getRecords().isEmpty()) {
            Page<PendingQualificationUserVO> empty = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        List<Long> userIds = raw.getRecords().stream().map(UserProfile::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, BtgUser> userMap = userIds.isEmpty()
                ? Map.of()
                : btgUserMapper.selectList(new LambdaQueryWrapper<BtgUser>().in(BtgUser::getId, userIds)).stream()
                        .collect(Collectors.toMap(BtgUser::getId, u -> u, (a, b) -> a));

        List<PendingQualificationUserVO> vos = raw.getRecords().stream().map(prof -> {
            BtgUser u = userMap.get(prof.getUserId());
            return PendingQualificationUserVO.builder()
                    .userId(prof.getUserId())
                    .mobile(u != null ? u.getMobile() : null)
                    .nickname(u != null ? u.getNickname() : null)
                    .realName(prof.getRealName())
                    .serverName(prof.getServerName())
                    .tradingAccountId(prof.getTradingAccountId())
                    .exchangeUid(prof.getExchangeUid())
                    .principalAmount(prof.getPrincipalAmount())
                    .qualificationStatus(prof.getQualificationStatus())
                    .createdAt(prof.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        Page<PendingQualificationUserVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(vos);
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveQualification(Long userId, Long operatorUserId, String remark) {
        UserProfile profile = requirePendingProfile(userId);
        LocalDateTime now = LocalDateTime.now();
        String r = normalizeRemark(remark);

        UserProfile patch = new UserProfile();
        patch.setId(profile.getId());
        patch.setQualificationStatus(QualificationStatusEnum.APPROVED);
        patch.setQualificationAuditTime(now);
        patch.setQualificationAuditBy(operatorUserId);
        patch.setQualificationAuditRemark(r);
        userProfileMapper.updateById(patch);

        auditLogService.log(AuditBusinessType.USER_QUALIFICATION, userId, AuditAction.APPROVE, operatorUserId, r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectQualification(Long userId, Long operatorUserId, String remark) {
        UserProfile profile = requirePendingProfile(userId);
        LocalDateTime now = LocalDateTime.now();
        String r = normalizeRemark(remark);

        UserProfile patch = new UserProfile();
        patch.setId(profile.getId());
        patch.setQualificationStatus(QualificationStatusEnum.REJECTED);
        patch.setQualificationAuditTime(now);
        patch.setQualificationAuditBy(operatorUserId);
        patch.setQualificationAuditRemark(r);
        userProfileMapper.updateById(patch);

        auditLogService.log(AuditBusinessType.USER_QUALIFICATION, userId, AuditAction.REJECT, operatorUserId, r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitQualification(Long currentUserId, String remark) {
        if (currentUserId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户未登录");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, currentUserId)
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户资料不存在");
        }
        if (profile.getQualificationStatus() != QualificationStatusEnum.REJECTED) {
            throw new BizException(ResultCode.CONFLICT, "仅资格审核被拒绝后可重新提交");
        }
        assertProfileCompleteForQualificationResubmit(profile);

        int prev = profile.getQualificationSubmitCount() == null ? 1 : profile.getQualificationSubmitCount();
        LocalDateTime now = LocalDateTime.now();
        String r = normalizeRemark(remark);

        userProfileMapper.update(
                null,
                new LambdaUpdateWrapper<UserProfile>()
                        .eq(UserProfile::getId, profile.getId())
                        .set(UserProfile::getQualificationStatus, QualificationStatusEnum.PENDING)
                        .set(UserProfile::getQualificationAuditTime, null)
                        .set(UserProfile::getQualificationAuditBy, null)
                        .set(UserProfile::getQualificationAuditRemark, null)
                        .set(UserProfile::getQualificationSubmitCount, prev + 1)
                        .set(UserProfile::getQualificationLastSubmitTime, now));

        auditLogService.log(AuditBusinessType.USER_QUALIFICATION, currentUserId, AuditAction.RESUBMIT, currentUserId, r);
    }

    private void assertProfileCompleteForQualificationResubmit(UserProfile p) {
        if (!StringUtils.hasText(p.getRealName())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请填写真实姓名后再提交资格审核");
        }
        if (!StringUtils.hasText(p.getIdCardFrontUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传身份证正面后再提交资格审核");
        }
        if (!StringUtils.hasText(p.getIdCardBackUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传身份证反面后再提交资格审核");
        }
        if (!StringUtils.hasText(p.getFacePhotoUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传人脸照片后再提交资格审核");
        }
        if (!StringUtils.hasText(p.getServerName())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请填写服务器名称后再提交资格审核");
        }
        if (!StringUtils.hasText(p.getTradingAccountId())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请填写交易账户后再提交资格审核");
        }
        if (!StringUtils.hasText(p.getExchangeUid())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请填写交易所 UID 后再提交资格审核");
        }
        BigDecimal principal = MoneyUtil.money(p.getPrincipalAmount());
        if (principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "请填写有效的底仓本金后再提交资格审核");
        }
    }

    private UserProfile requirePendingProfile(Long userId) {
        if (userId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户 id 无效");
        }
        BtgUser user = btgUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户资料不存在");
        }
        if (profile.getQualificationStatus() != QualificationStatusEnum.PENDING) {
            throw new BizException(ResultCode.CONFLICT, "当前不在待系统管理员审核状态");
        }
        return profile;
    }

    private static String normalizeRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        return remark.trim();
    }
}

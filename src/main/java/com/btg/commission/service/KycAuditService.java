package com.btg.commission.service;

import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.kyc.KycAuditRequest;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.KycStatus;
import com.btg.commission.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 下级 KYC 待审核（PENDING）时，其直属上级与任意上级均可审核通过/拒绝。
 */
@Service
@RequiredArgsConstructor
public class KycAuditService {

    private final UserService userService;
    private final UserProfileMapper userProfileMapper;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long auditorUserId, KycAuditRequest req) {
        assertUpstream(auditorUserId, req.getTargetUserId());
        UserProfile profile = userProfileMapper.selectByUserIdForUpdate(req.getTargetUserId());
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户尚未建立资料记录");
        }
        if (profile.getKycStatus() == KycStatus.APPROVED) {
            return;
        }
        if (profile.getKycStatus() != KycStatus.PENDING) {
            throw new BizException(ResultCode.CONFLICT, "仅待审核状态可通过审核");
        }
        profile.setKycStatus(KycStatus.APPROVED);
        profile.setKycAuditTime(LocalDateTime.now());
        profile.setKycAuditRemark(req.getRemark());
        userProfileMapper.updateById(profile);
        auditLogService.log(AuditBusinessType.USER_PROFILE_KYC, req.getTargetUserId(),
                AuditAction.APPROVE, auditorUserId, req.getRemark());
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long auditorUserId, KycAuditRequest req) {
        assertUpstream(auditorUserId, req.getTargetUserId());
        UserProfile profile = userProfileMapper.selectByUserIdForUpdate(req.getTargetUserId());
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户尚未建立资料记录");
        }
        if (profile.getKycStatus() == KycStatus.REJECTED) {
            return;
        }
        if (profile.getKycStatus() == KycStatus.APPROVED) {
            throw new BizException(ResultCode.CONFLICT, "已通过审核，不可拒绝");
        }
        if (profile.getKycStatus() != KycStatus.PENDING) {
            throw new BizException(ResultCode.CONFLICT, "仅待审核状态可拒绝");
        }
        profile.setKycStatus(KycStatus.REJECTED);
        profile.setKycAuditTime(LocalDateTime.now());
        profile.setKycAuditRemark(req.getRemark());
        userProfileMapper.updateById(profile);
        auditLogService.log(AuditBusinessType.USER_PROFILE_KYC, req.getTargetUserId(),
                AuditAction.REJECT, auditorUserId, req.getRemark());
    }

    private void assertUpstream(Long auditorUserId, Long targetUserId) {
        if (!userService.isUpstreamOf(auditorUserId, targetUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅被审核用户的直属上级或上级可操作");
        }
    }
}

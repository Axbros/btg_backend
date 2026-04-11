package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.ReplenishmentApproveDTO;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.service.AuditLogService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.ReplenishmentApplyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ReplenishmentServiceImpl implements ReplenishmentService {

    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final UserProfileMapper userProfileMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, ReplenishmentApplyDTO dto) {
        if (replenishmentApplyMapper.existsBlockingNewReplenishmentByUserId(userId)) {
            throw new BizException(ResultCode.CONFLICT, "存在进行中的补仓申请，请勿重复提交");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "请先完善用户资料");
        }
        BigDecimal principal = MoneyUtil.money(profile.getPrincipalAmount());
        BigDecimal balance = MoneyUtil.money(dto.getBalanceAmount());
        BigDecimal replenish = MoneyUtil.money(principal.subtract(balance));
        if (replenish.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "补仓额度须大于 0（底仓本金应大于当前余额）");
        }
        if (!StringUtils.hasText(dto.getBalanceScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传余额截图");
        }
        BtgReplenishmentApply row = new BtgReplenishmentApply();
        row.setApplyNo(nextApplyNo());
        row.setUserId(userId);
        row.setPrincipalAmount(principal);
        row.setBalanceAmount(balance);
        row.setReplenishAmount(replenish);
        row.setBalanceScreenshotUrl(dto.getBalanceScreenshotUrl().trim());
        row.setStatus(ReplenishmentStatusEnum.PENDING_AUDIT);
        row.setApprovedAmount(MoneyUtil.money(null));
        row.setRepaidAmount(MoneyUtil.money(null));
        row.setPendingRepayAmount(MoneyUtil.money(null));
        row.setRemainingAmount(MoneyUtil.money(null));
        row.setSubmitTime(LocalDateTime.now());
        replenishmentApplyMapper.insert(row);
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.SUBMIT, userId, null);
        return row.getId();
    }

    @Override
    public Page<ReplenishmentApplyVO> pageMine(Long userId, long page, long size) {
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .orderByDesc(BtgReplenishmentApply::getSubmitTime));
        Page<ReplenishmentApplyVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(ReplenishmentServiceImpl::toVo).toList());
        return out;
    }

    @Override
    public ReplenishmentApplyVO current(Long userId) {
        BtgReplenishmentApply one = replenishmentApplyMapper.selectOne(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .in(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.APPROVED, ReplenishmentStatusEnum.PARTIALLY_REPAID)
                .orderByDesc(BtgReplenishmentApply::getId)
                .last("LIMIT 1"));
        return one == null ? null : toVo(one);
    }

    @Override
    public Page<ReplenishmentApplyVO> pagePendingForAdmin(long page, long size) {
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.PENDING_AUDIT)
                .orderByAsc(BtgReplenishmentApply::getSubmitTime));
        Page<ReplenishmentApplyVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(ReplenishmentServiceImpl::toVo).toList());
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveForAdmin(Long applyId, Long adminUserId, ReplenishmentApproveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTransferScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传资方补仓转账凭证");
        }
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_AUDIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可审核通过");
        }
        BigDecimal replenish = MoneyUtil.money(row.getReplenishAmount());
        row.setStatus(ReplenishmentStatusEnum.APPROVED);
        row.setApprovedAmount(replenish);
        row.setRemainingAmount(replenish);
        row.setRepaidAmount(MoneyUtil.money(null));
        row.setPendingRepayAmount(MoneyUtil.money(null));
        row.setTransferScreenshotUrl(dto.getTransferScreenshotUrl().trim());
        row.setTransferRemark(trimOrNull(dto.getTransferRemark()));
        row.setAuditBy(adminUserId);
        row.setAuditTime(LocalDateTime.now());
        row.setAuditRemark(null);
        replenishmentApplyMapper.updateById(row);
        auditLogService.log(
                AuditBusinessType.REPLENISHMENT_APPLY,
                row.getId(),
                AuditAction.APPROVE,
                adminUserId,
                "审核通过补仓申请，并上传资方转账凭证");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectForAdmin(Long applyId, Long adminUserId, String remark) {
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_AUDIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
        }
        row.setStatus(ReplenishmentStatusEnum.REJECTED);
        row.setAuditBy(adminUserId);
        row.setAuditTime(LocalDateTime.now());
        row.setAuditRemark(trimOrNull(remark));
        replenishmentApplyMapper.updateById(row);
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.REJECT, adminUserId, remark);
    }

    private static String trimOrNull(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        return remark.trim();
    }

    private static String nextApplyNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "RF" + ts + rnd;
    }

    private static ReplenishmentApplyVO toVo(BtgReplenishmentApply e) {
        return ReplenishmentApplyVO.builder()
                .id(e.getId())
                .applyNo(e.getApplyNo())
                .userId(e.getUserId())
                .principalAmount(e.getPrincipalAmount())
                .balanceAmount(e.getBalanceAmount())
                .replenishAmount(e.getReplenishAmount())
                .balanceScreenshotUrl(e.getBalanceScreenshotUrl())
                .transferScreenshotUrl(e.getTransferScreenshotUrl())
                .transferRemark(e.getTransferRemark())
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .approvedAmount(e.getApprovedAmount())
                .repaidAmount(e.getRepaidAmount())
                .pendingRepayAmount(e.getPendingRepayAmount())
                .remainingAmount(e.getRemainingAmount())
                .submitTime(e.getSubmitTime())
                .auditTime(e.getAuditTime())
                .auditBy(e.getAuditBy())
                .auditRemark(e.getAuditRemark())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.dto.v1.RepayResubmitRequest;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgReplenishmentRepayApply;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;
import com.btg.commission.enums.RepayStatusEnum;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.BtgReplenishmentRepayApplyMapper;
import com.btg.commission.service.AuditLogService;
import com.btg.commission.service.BusinessFlowLogService;
import com.btg.commission.service.RepayWorkflowService;
import com.btg.commission.service.UserQualificationGateService;
import com.btg.commission.util.MoneyUtil;
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
public class RepayWorkflowServiceImpl implements RepayWorkflowService {

    private final BtgReplenishmentRepayApplyMapper repayApplyMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final AuditLogService auditLogService;
    private final BusinessFlowLogService businessFlowLogService;
    private final UserQualificationGateService userQualificationGateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitRepay(Long userId, RepayApplyDTO dto) {
        userQualificationGateService.requireApprovedForFormalBusiness(userId);
        BtgReplenishmentApply parent = loadReplenishmentForSubmit(dto.getReplenishApplyId(), userId);

        if (!StringUtils.hasText(dto.getRepayScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传归仓转账截图");
        }
        BigDecimal repay = MoneyUtil.money(dto.getRepayAmount());
        if (repay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额须大于 0");
        }
        BigDecimal remaining = MoneyUtil.money(parent.getRemainingAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        if (repay.compareTo(remaining) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额不能超过剩余应还金额");
        }
        BigDecimal ceiling = MoneyUtil.money(remaining.subtract(pending));
        if (repay.compareTo(ceiling) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额超过可归仓上限（含待审核归仓）");
        }

        Long capitalUserId = parent.getAssignedCapitalUserId();
        if (capitalUserId == null) {
            throw new BizException(ResultCode.CONFLICT, "该补仓单尚未指定资方执行人，无法归仓");
        }

        BtgReplenishmentRepayApply row = new BtgReplenishmentRepayApply();
        row.setRepayNo(nextRepayNo());
        row.setReplenishApplyId(dto.getReplenishApplyId());
        row.setUserId(userId);
        row.setCapitalUserId(capitalUserId);
        row.setCapitalReceiverUid(trimOrNull(parent.getCapitalReceiverUid()));
        row.setRepayAmount(repay);
        row.setRepayScreenshotUrl(dto.getRepayScreenshotUrl().trim());
        row.setStatus(RepayStatusEnum.PENDING_CAPITAL_REVIEW);
        row.setSubmitTime(LocalDateTime.now());
        row.setSubmitVersion(1);
        row.setCurrentHandlerUserId(capitalUserId);
        row.setReturnedToUser(false);
        row.setFlowStatus("PENDING_CAPITAL_REVIEW");
        repayApplyMapper.insert(row);

        parent.setPendingRepayAmount(MoneyUtil.money(pending.add(repay)));
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, row.getId(), AuditAction.SUBMIT, userId, null);
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_REPAY_APPLY,
                row.getId(),
                dto.getReplenishApplyId(),
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.SUBMIT,
                RepayStatusEnum.PENDING_CAPITAL_REVIEW.name(),
                1,
                null,
                userId);
        return row.getId();
    }

    private BtgReplenishmentApply loadReplenishmentForSubmit(Long replenishApplyId, Long userId) {
        if (replenishApplyId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "补仓申请ID不能为空");
        }
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectById(replenishApplyId);
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在或已删除");
        }
        if (!userId.equals(parent.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权对该补仓单提交归仓");
        }
        if (parent.getStatus() != ReplenishmentStatusEnum.SUCCESS) {
            throw new BizException(ResultCode.CONFLICT, "该补仓单未处于可归仓状态");
        }
        BigDecimal remaining = MoneyUtil.money(parent.getRemainingAmount());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.CONFLICT, "该补仓单已无可归仓金额");
        }
        return parent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitRepay(Long userId, Long repayApplyId, RepayResubmitRequest req) {
        userQualificationGateService.requireApprovedForFormalBusiness(userId);
        BtgReplenishmentRepayApply repay = repayApplyMapper.selectById(repayApplyId);
        if (repay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (!userId.equals(repay.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申请人可重新提交");
        }
        if (repay.getStatus() != RepayStatusEnum.RETURNED_TO_APPLICANT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可重新提交");
        }
        BtgReplenishmentApply parent = loadReplenishmentForSubmit(repay.getReplenishApplyId(), userId);

        if (!StringUtils.hasText(req.getRepayScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传归仓转账截图");
        }
        BigDecimal newRepay = MoneyUtil.money(req.getRepayAmount());
        if (newRepay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额须大于 0");
        }
        BigDecimal remaining = MoneyUtil.money(parent.getRemainingAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        BigDecimal ceiling = MoneyUtil.money(remaining.subtract(pending));
        if (newRepay.compareTo(ceiling) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额超过可归仓上限（含待审核归仓）");
        }

        Long capitalUserId = repay.getCapitalUserId() != null ? repay.getCapitalUserId() : parent.getAssignedCapitalUserId();
        if (capitalUserId == null) {
            throw new BizException(ResultCode.CONFLICT, "该补仓单尚未指定资方执行人，无法归仓");
        }
        String capitalUid = StringUtils.hasText(repay.getCapitalReceiverUid())
                ? repay.getCapitalReceiverUid()
                : trimOrNull(parent.getCapitalReceiverUid());

        int nextVer = (repay.getSubmitVersion() == null ? 1 : repay.getSubmitVersion()) + 1;
        BtgReplenishmentRepayApply patch = new BtgReplenishmentRepayApply();
        patch.setId(repayApplyId);
        patch.setCapitalUserId(capitalUserId);
        patch.setCapitalReceiverUid(capitalUid);
        patch.setRepayAmount(newRepay);
        patch.setRepayScreenshotUrl(req.getRepayScreenshotUrl().trim());
        patch.setStatus(RepayStatusEnum.PENDING_CAPITAL_REVIEW);
        patch.setSubmitTime(LocalDateTime.now());
        patch.setSubmitVersion(nextVer);
        patch.setCurrentHandlerUserId(capitalUserId);
        patch.setReturnedToUser(false);
        patch.setFlowStatus("PENDING_CAPITAL_REVIEW");
        patch.setLastRejectReason(null);
        patch.setLastRejectTime(null);
        patch.setLastRejectBy(null);
        patch.setAuditRemark(null);
        patch.setAuditBy(null);
        patch.setAuditTime(null);
        repayApplyMapper.updateById(patch);

        parent.setPendingRepayAmount(MoneyUtil.money(pending.add(newRepay)));
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, repayApplyId, AuditAction.SUBMIT, userId, "resubmit");
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_REPAY_APPLY,
                repayApplyId,
                repay.getReplenishApplyId(),
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.RESUBMIT,
                RepayStatusEnum.PENDING_CAPITAL_REVIEW.name(),
                nextVer,
                null,
                userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRepay(Long capitalUserId, Long repayApplyId, String remark) {
        BtgReplenishmentRepayApply repay = repayApplyMapper.selectById(repayApplyId);
        if (repay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (!capitalUserId.equals(repay.getCapitalUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅补仓执行方可审核该归仓申请");
        }
        if (repay.getStatus() != RepayStatusEnum.PENDING_CAPITAL_REVIEW) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可审核通过");
        }
        if (repay.getReplenishApplyId() == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectById(repay.getReplenishApplyId());
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        if (parent.getStatus() != ReplenishmentStatusEnum.SUCCESS) {
            throw new BizException(ResultCode.CONFLICT, "关联补仓单状态异常");
        }
        BigDecimal r = MoneyUtil.money(repay.getRepayAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        if (pending.compareTo(r) < 0) {
            throw new BizException(ResultCode.CONFLICT, "待审核归仓金额数据异常");
        }

        BigDecimal newPending = MoneyUtil.money(pending.subtract(r));
        BigDecimal newRepaid = MoneyUtil.money(MoneyUtil.money(parent.getRepaidAmount()).add(r));
        BigDecimal approved = MoneyUtil.money(parent.getApprovedAmount());
        BigDecimal newRemaining = MoneyUtil.money(approved.subtract(newRepaid));

        repay.setStatus(RepayStatusEnum.APPROVED);
        repay.setAuditBy(capitalUserId);
        repay.setAuditTime(LocalDateTime.now());
        repay.setAuditRemark(trimOrNull(remark));
        repayApplyMapper.updateById(repay);

        parent.setPendingRepayAmount(newPending);
        parent.setRepaidAmount(newRepaid);
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            long otherPending = repayApplyMapper.selectCount(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                    .eq(BtgReplenishmentRepayApply::getReplenishApplyId, parent.getId())
                    .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.PENDING_CAPITAL_REVIEW)
                    .ne(BtgReplenishmentRepayApply::getId, repayApplyId));
            if (otherPending > 0) {
                throw new BizException(ResultCode.CONFLICT, "仍有其他待审核归仓，无法结案补仓单");
            }
            parent.setRemainingAmount(MoneyUtil.money(null));
            parent.setStatus(ReplenishmentStatusEnum.CLOSED);
        } else {
            parent.setRemainingAmount(newRemaining);
            parent.setStatus(ReplenishmentStatusEnum.SUCCESS);
        }
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, repay.getId(), AuditAction.APPROVE, capitalUserId, remark);
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_REPAY_APPLY,
                repay.getId(),
                repay.getReplenishApplyId(),
                repay.getUserId(),
                FlowNodeRole.CAPITAL,
                FlowAction.APPROVE,
                RepayStatusEnum.APPROVED.name(),
                repay.getSubmitVersion() == null ? 1 : repay.getSubmitVersion(),
                remark,
                capitalUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRepay(Long capitalUserId, Long repayApplyId, String remark) {
        BtgReplenishmentRepayApply repay = repayApplyMapper.selectById(repayApplyId);
        if (repay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (!capitalUserId.equals(repay.getCapitalUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅补仓执行方可拒绝该归仓申请");
        }
        if (repay.getStatus() != RepayStatusEnum.PENDING_CAPITAL_REVIEW) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
        }
        if (repay.getReplenishApplyId() == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectById(repay.getReplenishApplyId());
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        BigDecimal r = MoneyUtil.money(repay.getRepayAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        if (pending.compareTo(r) < 0) {
            throw new BizException(ResultCode.CONFLICT, "待审核归仓金额数据异常");
        }

        LocalDateTime now = LocalDateTime.now();
        BtgReplenishmentRepayApply patch = new BtgReplenishmentRepayApply();
        patch.setId(repayApplyId);
        patch.setStatus(RepayStatusEnum.RETURNED_TO_APPLICANT);
        patch.setAuditBy(capitalUserId);
        patch.setAuditTime(now);
        patch.setAuditRemark(trimOrNull(remark));
        patch.setCurrentHandlerUserId(repay.getUserId());
        patch.setReturnedToUser(true);
        patch.setFlowStatus("RETURNED_TO_APPLICANT");
        patch.setLastRejectReason(trimOrNull(remark));
        patch.setLastRejectTime(now);
        patch.setLastRejectBy(capitalUserId);
        repayApplyMapper.updateById(patch);

        parent.setPendingRepayAmount(MoneyUtil.money(pending.subtract(r)));
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, repay.getId(), AuditAction.REJECT, capitalUserId, remark);
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_REPAY_APPLY,
                repay.getId(),
                repay.getReplenishApplyId(),
                repay.getUserId(),
                FlowNodeRole.CAPITAL,
                FlowAction.RETURN_TO_APPLICANT,
                RepayStatusEnum.RETURNED_TO_APPLICANT.name(),
                repay.getSubmitVersion() == null ? 1 : repay.getSubmitVersion(),
                remark,
                capitalUserId);
    }

    private static String trimOrNull(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        return remark.trim();
    }

    private static String nextRepayNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "WA" + ts + rnd;
    }
}

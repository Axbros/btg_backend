package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.ReplenishmentCapitalSubmitRequest;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.enums.ArrivalConfirmStatusEnum;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.service.AuditLogService;
import com.btg.commission.service.BusinessFlowLogService;
import com.btg.commission.service.ReplenishmentWorkflowService;
import com.btg.commission.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReplenishmentWorkflowServiceImpl implements ReplenishmentWorkflowService {

    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final BtgUserMapper btgUserMapper;
    private final AuditLogService auditLogService;
    private final BusinessFlowLogService businessFlowLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveByAdmin(Long applyId, Long adminUserId, String remark) {
        BtgReplenishmentApply row = requireApply(applyId);
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可管理员审核通过");
        }
        LocalDateTime now = LocalDateTime.now();
        replenishmentApplyMapper.update(
                null,
                new LambdaUpdateWrapper<BtgReplenishmentApply>()
                        .eq(BtgReplenishmentApply::getId, applyId)
                        .set(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL)
                        .set(BtgReplenishmentApply::getAssignedCapitalUserId, null)
                        .set(BtgReplenishmentApply::getAuditBy, adminUserId)
                        .set(BtgReplenishmentApply::getAuditTime, now)
                        .set(BtgReplenishmentApply::getAuditRemark, trimOrNull(remark))
                        .set(BtgReplenishmentApply::getCurrentHandlerUserId, adminUserId)
                        .set(BtgReplenishmentApply::getFlowStatus, ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL.name()));
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.APPROVE, adminUserId, trimOrNull(remark));
        appendFlow(row, FlowNodeRole.ROOT, FlowAction.APPROVE, ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL, adminUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByAdmin(Long applyId, Long adminUserId, String remark) {
        BtgReplenishmentApply row = requireApply(applyId);
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可管理员拒绝");
        }
        LocalDateTime now = LocalDateTime.now();
        replenishmentApplyMapper.update(
                null,
                new LambdaUpdateWrapper<BtgReplenishmentApply>()
                        .eq(BtgReplenishmentApply::getId, applyId)
                        .set(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.REJECTED)
                        .set(BtgReplenishmentApply::getAuditBy, adminUserId)
                        .set(BtgReplenishmentApply::getAuditTime, now)
                        .set(BtgReplenishmentApply::getAuditRemark, trimOrNull(remark))
                        .set(BtgReplenishmentApply::getCurrentHandlerUserId, row.getUserId())
                        .set(BtgReplenishmentApply::getFlowStatus, ReplenishmentStatusEnum.REJECTED.name())
                        .set(BtgReplenishmentApply::getLastRejectReason, trimOrNull(remark))
                        .set(BtgReplenishmentApply::getLastRejectTime, now)
                        .set(BtgReplenishmentApply::getLastRejectBy, adminUserId)
                        .set(BtgReplenishmentApply::getTransferScreenshotUrl, null)
                        .set(BtgReplenishmentApply::getTransferRemark, null)
                        .set(BtgReplenishmentApply::getAssignedCapitalUserId, null)
                        .set(BtgReplenishmentApply::getAssignedBy, null)
                        .set(BtgReplenishmentApply::getAssignedTime, null)
                        .set(BtgReplenishmentApply::getAssignRemark, null)
                        .set(BtgReplenishmentApply::getCapitalSubmitTime, null)
                        .set(BtgReplenishmentApply::getCapitalSubmitRemark, null)
                        .set(BtgReplenishmentApply::getCapitalReceiverUid, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmStatus, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmTime, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmBy, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmRemark, null));
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.REJECT, adminUserId, trimOrNull(remark));
        appendFlow(row, FlowNodeRole.ROOT, FlowAction.REJECT, ReplenishmentStatusEnum.REJECTED, adminUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignCapital(Long applyId, Long adminUserId, Long capitalUserId, String remark) {
        if (capitalUserId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "资方执行人不能为空");
        }
        BtgUser capital = btgUserMapper.selectById(capitalUserId);
        if (capital == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资方执行人不存在");
        }
        BtgReplenishmentApply row = requireApply(applyId);
        if (row.getStatus() != ReplenishmentStatusEnum.ASSIGNED_TO_CAPITAL) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可转派资方");
        }
        LocalDateTime now = LocalDateTime.now();
        replenishmentApplyMapper.update(
                null,
                new LambdaUpdateWrapper<BtgReplenishmentApply>()
                        .eq(BtgReplenishmentApply::getId, applyId)
                        .set(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT)
                        .set(BtgReplenishmentApply::getAssignedCapitalUserId, capitalUserId)
                        .set(BtgReplenishmentApply::getAssignedBy, adminUserId)
                        .set(BtgReplenishmentApply::getAssignedTime, now)
                        .set(BtgReplenishmentApply::getAssignRemark, trimOrNull(remark))
                        .set(BtgReplenishmentApply::getCurrentHandlerUserId, capitalUserId)
                        .set(BtgReplenishmentApply::getFlowStatus, ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT.name()));
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.ASSIGN, adminUserId, trimOrNull(remark));
        appendFlow(row, FlowNodeRole.ROOT, FlowAction.ASSIGN, ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT, adminUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void capitalSubmit(Long applyId, Long capitalUserId, ReplenishmentCapitalSubmitRequest dto) {
        if (dto == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求体不能为空");
        }
        BtgReplenishmentApply row = requireApply(applyId);
        if (!capitalUserId.equals(row.getAssignedCapitalUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅资方执行人可提交补仓凭证");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT
                && row.getStatus() != ReplenishmentStatusEnum.RETURNED_TO_CAPITAL) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可提交资方凭证");
        }
        LocalDateTime now = LocalDateTime.now();
        replenishmentApplyMapper.update(
                null,
                new LambdaUpdateWrapper<BtgReplenishmentApply>()
                        .eq(BtgReplenishmentApply::getId, applyId)
                        .set(BtgReplenishmentApply::getTransferScreenshotUrl, dto.getTransferScreenshotUrl().trim())
                        .set(BtgReplenishmentApply::getTransferRemark, trimOrNull(dto.getTransferRemark()))
                        .set(BtgReplenishmentApply::getCapitalSubmitTime, now)
                        .set(BtgReplenishmentApply::getCapitalSubmitRemark, trimOrNull(dto.getTransferRemark()))
                        .set(BtgReplenishmentApply::getCapitalReceiverUid, dto.getCapitalReceiverUid().trim())
                        .set(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM)
                        .set(BtgReplenishmentApply::getCurrentHandlerUserId, row.getUserId())
                        .set(BtgReplenishmentApply::getArrivalConfirmStatus, ArrivalConfirmStatusEnum.PENDING)
                        .set(BtgReplenishmentApply::getArrivalConfirmTime, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmBy, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmRemark, null)
                        .set(BtgReplenishmentApply::getFlowStatus, ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM.name()));
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.SUBMIT, capitalUserId, dto.getTransferRemark());
        appendFlow(row, FlowNodeRole.CAPITAL, FlowAction.SUBMIT, ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM, capitalUserId, dto.getTransferRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmArrival(Long applyId, Long applicantUserId, String remark) {
        BtgReplenishmentApply row = requireApply(applyId);
        if (!applicantUserId.equals(row.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申请人可确认到账");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可确认到账");
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal replenish = MoneyUtil.money(row.getReplenishAmount());
        replenishmentApplyMapper.update(
                null,
                new LambdaUpdateWrapper<BtgReplenishmentApply>()
                        .eq(BtgReplenishmentApply::getId, applyId)
                        .set(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.SUCCESS)
                        .set(BtgReplenishmentApply::getApprovedAmount, replenish)
                        .set(BtgReplenishmentApply::getRemainingAmount, replenish)
                        .set(BtgReplenishmentApply::getRepaidAmount, null)
                        .set(BtgReplenishmentApply::getPendingRepayAmount, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmStatus, ArrivalConfirmStatusEnum.CONFIRMED)
                        .set(BtgReplenishmentApply::getArrivalConfirmTime, now)
                        .set(BtgReplenishmentApply::getArrivalConfirmBy, applicantUserId)
                        .set(BtgReplenishmentApply::getArrivalConfirmRemark, trimOrNull(remark))
                        .set(BtgReplenishmentApply::getCurrentHandlerUserId, null)
                        .set(BtgReplenishmentApply::getFlowStatus, ReplenishmentStatusEnum.SUCCESS.name()));
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.APPROVE, applicantUserId, trimOrNull(remark));
        appendFlow(row, FlowNodeRole.APPLICANT, FlowAction.APPROVE, ReplenishmentStatusEnum.SUCCESS, applicantUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectArrival(Long applyId, Long applicantUserId, String remark) {
        BtgReplenishmentApply row = requireApply(applyId);
        if (!applicantUserId.equals(row.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申请人可拒绝到账");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_APPLICANT_CONFIRM) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝到账");
        }
        if (!StringUtils.hasText(remark)) {
            throw new BizException(ResultCode.BAD_REQUEST, "请填写拒绝原因");
        }
        LocalDateTime now = LocalDateTime.now();
        Long capitalId = row.getAssignedCapitalUserId();
        replenishmentApplyMapper.update(
                null,
                new LambdaUpdateWrapper<BtgReplenishmentApply>()
                        .eq(BtgReplenishmentApply::getId, applyId)
                        .set(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.RETURNED_TO_CAPITAL)
                        .set(BtgReplenishmentApply::getArrivalConfirmStatus, ArrivalConfirmStatusEnum.REJECTED)
                        .set(BtgReplenishmentApply::getArrivalConfirmTime, now)
                        .set(BtgReplenishmentApply::getArrivalConfirmBy, applicantUserId)
                        .set(BtgReplenishmentApply::getArrivalConfirmRemark, trimOrNull(remark))
                        .set(BtgReplenishmentApply::getCurrentHandlerUserId, capitalId)
                        .set(BtgReplenishmentApply::getFlowStatus, ReplenishmentStatusEnum.RETURNED_TO_CAPITAL.name()));
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.REJECT, applicantUserId, trimOrNull(remark));
        appendFlow(row, FlowNodeRole.APPLICANT, FlowAction.REJECT, ReplenishmentStatusEnum.RETURNED_TO_CAPITAL, applicantUserId, remark);
    }

    private BtgReplenishmentApply requireApply(Long applyId) {
        if (applyId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "申请ID无效");
        }
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        return row;
    }

    private void appendFlow(
            BtgReplenishmentApply row,
            FlowNodeRole role,
            FlowAction action,
            ReplenishmentStatusEnum newStatus,
            Long operatorUserId,
            String remark) {
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_APPLY,
                row.getId(),
                null,
                operatorUserId,
                role,
                action,
                newStatus.name(),
                row.getSubmitVersion() == null ? 1 : row.getSubmitVersion(),
                remark,
                operatorUserId);
    }

    private static String trimOrNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim();
    }
}

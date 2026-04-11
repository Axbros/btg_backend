package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgReplenishmentRepayApply;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.RepayStatusEnum;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.BtgReplenishmentRepayApplyMapper;
import com.btg.commission.service.AuditLogService;
import com.btg.commission.service.RepayService;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.RepayApplyVO;
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
public class RepayServiceImpl implements RepayService {

    private final BtgReplenishmentRepayApplyMapper repayApplyMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, RepayApplyDTO dto) {
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectOne(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .in(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.APPROVED, ReplenishmentStatusEnum.PARTIALLY_REPAID)
                .orderByDesc(BtgReplenishmentApply::getId)
                .last("LIMIT 1"));
        if (parent == null) {
            throw new BizException(ResultCode.CONFLICT, "无未结清补仓单，无法提交归仓申请");
        }
        if (!StringUtils.hasText(dto.getRepayScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传归仓转账截图");
        }
        BigDecimal repay = MoneyUtil.money(dto.getRepayAmount());
        if (repay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额须大于 0");
        }
        BigDecimal remaining = MoneyUtil.money(parent.getRemainingAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        BigDecimal ceiling = MoneyUtil.money(remaining.subtract(pending));
        if (repay.compareTo(ceiling) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额超过可归还上限（含待审核归仓）");
        }
        BtgReplenishmentRepayApply row = new BtgReplenishmentRepayApply();
        row.setRepayNo(nextRepayNo());
        row.setReplenishApplyId(parent.getId());
        row.setUserId(userId);
        row.setRepayAmount(repay);
        row.setRepayScreenshotUrl(dto.getRepayScreenshotUrl().trim());
        row.setStatus(RepayStatusEnum.PENDING_AUDIT);
        row.setSubmitTime(LocalDateTime.now());
        repayApplyMapper.insert(row);

        parent.setPendingRepayAmount(MoneyUtil.money(pending.add(repay)));
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, row.getId(), AuditAction.SUBMIT, userId, null);
        return row.getId();
    }

    @Override
    public Page<RepayApplyVO> pageMine(Long userId, long page, long size) {
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getUserId, userId)
                .orderByDesc(BtgReplenishmentRepayApply::getSubmitTime));
        Page<RepayApplyVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(RepayServiceImpl::toVo).toList());
        return out;
    }

    @Override
    public Page<RepayApplyVO> pagePendingForAdmin(long page, long size) {
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.PENDING_AUDIT)
                .orderByAsc(BtgReplenishmentRepayApply::getSubmitTime));
        Page<RepayApplyVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(RepayServiceImpl::toVo).toList());
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveForAdmin(Long repayApplyId, Long adminUserId, String remark) {
        BtgReplenishmentRepayApply repay = repayApplyMapper.selectById(repayApplyId);
        if (repay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (repay.getStatus() != RepayStatusEnum.PENDING_AUDIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可审核通过");
        }
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectById(repay.getReplenishApplyId());
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        if (parent.getStatus() != ReplenishmentStatusEnum.APPROVED
                && parent.getStatus() != ReplenishmentStatusEnum.PARTIALLY_REPAID) {
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

        parent.setPendingRepayAmount(newPending);
        parent.setRepaidAmount(newRepaid);
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            parent.setRemainingAmount(MoneyUtil.money(null));
            parent.setStatus(ReplenishmentStatusEnum.FULLY_REPAID);
        } else {
            parent.setRemainingAmount(newRemaining);
            parent.setStatus(ReplenishmentStatusEnum.PARTIALLY_REPAID);
        }
        replenishmentApplyMapper.updateById(parent);

        repay.setStatus(RepayStatusEnum.APPROVED);
        repay.setAuditBy(adminUserId);
        repay.setAuditTime(LocalDateTime.now());
        repay.setAuditRemark(trimOrNull(remark));
        repayApplyMapper.updateById(repay);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, repay.getId(), AuditAction.APPROVE, adminUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectForAdmin(Long repayApplyId, Long adminUserId, String remark) {
        BtgReplenishmentRepayApply repay = repayApplyMapper.selectById(repayApplyId);
        if (repay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (repay.getStatus() != RepayStatusEnum.PENDING_AUDIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
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
        parent.setPendingRepayAmount(MoneyUtil.money(pending.subtract(r)));
        replenishmentApplyMapper.updateById(parent);

        repay.setStatus(RepayStatusEnum.REJECTED);
        repay.setAuditBy(adminUserId);
        repay.setAuditTime(LocalDateTime.now());
        repay.setAuditRemark(trimOrNull(remark));
        repayApplyMapper.updateById(repay);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, repay.getId(), AuditAction.REJECT, adminUserId, remark);
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

    private static RepayApplyVO toVo(BtgReplenishmentRepayApply e) {
        return RepayApplyVO.builder()
                .id(e.getId())
                .repayNo(e.getRepayNo())
                .replenishApplyId(e.getReplenishApplyId())
                .userId(e.getUserId())
                .repayAmount(e.getRepayAmount())
                .repayScreenshotUrl(e.getRepayScreenshotUrl())
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .submitTime(e.getSubmitTime())
                .auditTime(e.getAuditTime())
                .auditBy(e.getAuditBy())
                .auditRemark(e.getAuditRemark())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

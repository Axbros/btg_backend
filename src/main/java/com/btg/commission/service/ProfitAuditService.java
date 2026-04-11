package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.CommissionRecord;
import com.btg.commission.entity.ProfitRecord;
import com.btg.commission.entity.UserAccountSummary;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.CommissionRecordStatus;
import com.btg.commission.enums.ProfitRecordStatus;
import com.btg.commission.mapper.CommissionRecordMapper;
import com.btg.commission.mapper.ProfitRecordMapper;
import com.btg.commission.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审核通过生成佣金流水；拒绝不生成。事务 + 行锁 + 状态校验实现幂等。
 * 提交时：下级 pending_out += 盈利×(1−比例)（commission_amount），下级 pending_in += 盈利×比例（net_amount），上级 pending_in += 盈利×(1−比例)。
 * 通过时：下级 total_profit += net_amount，上级 total_profit += commission_amount；下级 total_commission_out、上级 total_commission_in += commission_amount；pending 三者核销。
 */
@Service
@RequiredArgsConstructor
public class ProfitAuditService {

    private final ProfitRecordMapper profitRecordMapper;
    private final CommissionRecordMapper commissionRecordMapper;
    private final UserAccountSummaryService userAccountSummaryService;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long profitRecordId, Long auditorId, String remark) {
        approve(profitRecordId, auditorId, remark, false);
    }

    /**
     * @param onlyDirectReferrer true 时仅允许 {@code auditorId} 等于申报单上的直属推荐人 {@code referrer_user_id}
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long profitRecordId, Long auditorId, String remark, boolean onlyDirectReferrer) {
        ProfitRecord pr = profitRecordMapper.selectByIdForUpdate(profitRecordId);
        if (pr == null) {
            throw new BizException(ResultCode.NOT_FOUND, "profit record not found");
        }
        if (onlyDirectReferrer && !auditorId.equals(pr.getReferrerUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申报人直属上级可审核");
        }
        if (pr.getStatus() == ProfitRecordStatus.APPROVED) {
            return;
        }
        if (pr.getStatus() == ProfitRecordStatus.REJECTED) {
            throw new BizException(ResultCode.CONFLICT, "record already rejected");
        }
        if (pr.getStatus() != ProfitRecordStatus.PENDING) {
            throw new BizException(ResultCode.CONFLICT, "invalid status");
        }

        Long existing = commissionRecordMapper.selectCount(new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getProfitRecordId, profitRecordId));
        if (existing != null && existing > 0) {
            ProfitRecord patch = new ProfitRecord();
            patch.setId(pr.getId());
            patch.setStatus(ProfitRecordStatus.APPROVED);
            patch.setAuditTime(LocalDateTime.now());
            patch.setAuditBy(auditorId);
            patch.setAuditRemark(remark);
            profitRecordMapper.updateById(patch);
            return;
        }

        long p = pr.getUserId();
        long r = pr.getReferrerUserId();
        userAccountSummaryService.lockByUserId(Math.min(p, r));
        userAccountSummaryService.lockByUserId(Math.max(p, r));

        BigDecimal shareToReferrer = MoneyUtil.money(pr.getCommissionAmount());
        BigDecimal childRetainedShare = MoneyUtil.money(pr.getNetAmount());

        userAccountSummaryService.subtractPendingOnProfitResolved(
                pr.getUserId(), pr.getReferrerUserId(), shareToReferrer, childRetainedShare);

        UserAccountSummary playerRow = userAccountSummaryService.lockByUserId(pr.getUserId());
        UserAccountSummary referrerRow = userAccountSummaryService.lockByUserId(pr.getReferrerUserId());

        CommissionRecord cr = new CommissionRecord();
        cr.setProfitRecordId(pr.getId());
        cr.setFromUserId(pr.getUserId());
        cr.setToUserId(pr.getReferrerUserId());
        cr.setStrategyId(pr.getStrategyId());
        cr.setCommissionRate(pr.getCommissionRate());
        cr.setProfitAmount(pr.getProfitAmount());
        cr.setCommissionAmount(shareToReferrer);
        cr.setStatus(CommissionRecordStatus.CONFIRMED);
        cr.setConfirmedTime(LocalDateTime.now());
        commissionRecordMapper.insert(cr);

        playerRow.setTotalProfitAmount(MoneyUtil.money(
                playerRow.getTotalProfitAmount().add(childRetainedShare)));
        playerRow.setTotalCommissionOutAmount(MoneyUtil.money(
                playerRow.getTotalCommissionOutAmount().add(shareToReferrer)));
        referrerRow.setTotalProfitAmount(MoneyUtil.money(
                referrerRow.getTotalProfitAmount().add(shareToReferrer)));
        referrerRow.setTotalCommissionInAmount(MoneyUtil.money(
                referrerRow.getTotalCommissionInAmount().add(shareToReferrer)));

        userAccountSummaryService.persist(playerRow);
        userAccountSummaryService.persist(referrerRow);

        pr.setStatus(ProfitRecordStatus.APPROVED);
        pr.setAuditTime(LocalDateTime.now());
        pr.setAuditBy(auditorId);
        pr.setAuditRemark(remark);
        profitRecordMapper.updateById(pr);

        auditLogService.log(AuditBusinessType.PROFIT_RECORD, pr.getId(), AuditAction.APPROVE, auditorId, remark);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long profitRecordId, Long auditorId, String remark) {
        reject(profitRecordId, auditorId, remark, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long profitRecordId, Long auditorId, String remark, boolean onlyDirectReferrer) {
        ProfitRecord pr = profitRecordMapper.selectByIdForUpdate(profitRecordId);
        if (pr == null) {
            throw new BizException(ResultCode.NOT_FOUND, "profit record not found");
        }
        if (onlyDirectReferrer && !auditorId.equals(pr.getReferrerUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申报人直属上级可审核");
        }
        if (pr.getStatus() == ProfitRecordStatus.REJECTED) {
            return;
        }
        if (pr.getStatus() == ProfitRecordStatus.APPROVED) {
            throw new BizException(ResultCode.CONFLICT, "cannot reject approved record");
        }
        if (pr.getStatus() != ProfitRecordStatus.PENDING) {
            throw new BizException(ResultCode.CONFLICT, "invalid status");
        }

        long p = pr.getUserId();
        long r = pr.getReferrerUserId();
        userAccountSummaryService.lockByUserId(Math.min(p, r));
        userAccountSummaryService.lockByUserId(Math.max(p, r));

        BigDecimal shareToReferrer = MoneyUtil.money(pr.getCommissionAmount());
        BigDecimal childRetainedShare = MoneyUtil.money(pr.getNetAmount());

        userAccountSummaryService.subtractPendingOnProfitResolved(
                pr.getUserId(), pr.getReferrerUserId(), shareToReferrer, childRetainedShare);

        pr.setStatus(ProfitRecordStatus.REJECTED);
        pr.setAuditTime(LocalDateTime.now());
        pr.setAuditBy(auditorId);
        pr.setAuditRemark(remark);
        profitRecordMapper.updateById(pr);

        auditLogService.log(AuditBusinessType.PROFIT_RECORD, pr.getId(), AuditAction.REJECT, auditorId, remark);
    }
}

package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.profit.ProfitSubmitRequest;
import com.btg.commission.entity.CommissionStrategy;
import com.btg.commission.entity.ProfitRecord;
import com.btg.commission.entity.SysUser;
import com.btg.commission.entity.UserCommissionBinding;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.ProfitRecordStatus;
import com.btg.commission.mapper.CommissionStrategyMapper;
import com.btg.commission.mapper.ProfitRecordMapper;
import com.btg.commission.mapper.SysUserMapper;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.PageVo;
import com.btg.commission.vo.ProfitRecordVo;
import com.btg.commission.vo.ReferrerProfitListItemVo;
import com.btg.commission.vo.ReferrerProfitRecordDetailVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProfitRecordService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final ProfitRecordMapper profitRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final CommissionStrategyMapper commissionStrategyMapper;
    private final UserCommissionBindingService userCommissionBindingService;
    private final UserAccountSummaryService userAccountSummaryService;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, ProfitSubmitRequest req) {
        SysUser self = sysUserMapper.selectById(userId);
        if (self == null) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        Long refId = self.getReferrerUserId();
        if (refId == null || refId == 0) {
            throw new BizException(ResultCode.CONFLICT, "root user cannot submit profit declaration");
        }
        UserCommissionBinding binding = userCommissionBindingService.findActiveBinding(refId, userId);
        if (binding == null) {
            throw new BizException(ResultCode.CONFLICT, "referrer has not bound a commission strategy for you");
        }

        BigDecimal profitAmount = MoneyUtil.money(req.getProfitAmount());
        BigDecimal rate = MoneyUtil.rate(binding.getCommissionRateSnapshot());

        ProfitRecord pr = new ProfitRecord();
        pr.setRecordNo(nextRecordNo());
        pr.setUserId(userId);
        pr.setReferrerUserId(refId);
        pr.setStrategyId(binding.getStrategyId());
        pr.setProfitAmount(profitAmount);
        pr.setCommissionRate(rate);
        pr.setCommissionAmount(MoneyUtil.dueShareAmount(profitAmount, rate));
        pr.setNetAmount(MoneyUtil.commissionShareOfProfit(profitAmount, rate));
        pr.setProfitScreenshotUrl(req.getProfitScreenshotUrl());
        pr.setTransferScreenshotUrl(req.getTransferScreenshotUrl());
        pr.setStatus(ProfitRecordStatus.PENDING);
        pr.setSubmitTime(LocalDateTime.now());
        profitRecordMapper.insert(pr);

        long p = userId;
        long r = refId;
        userAccountSummaryService.lockByUserId(Math.min(p, r));
        userAccountSummaryService.lockByUserId(Math.max(p, r));
        userAccountSummaryService.applyPendingOnProfitSubmit(
                userId, refId, pr.getCommissionAmount(), pr.getNetAmount());

        auditLogService.log(AuditBusinessType.PROFIT_RECORD, pr.getId(), AuditAction.SUBMIT, userId, null);
        return pr.getId();
    }

    public Page<ProfitRecordVo> pageMine(Long userId, long page, long size) {
        Page<ProfitRecord> mp = new Page<>(page, size);
        profitRecordMapper.selectPage(mp, new LambdaQueryWrapper<ProfitRecord>()
                .eq(ProfitRecord::getUserId, userId)
                .orderByDesc(ProfitRecord::getSubmitTime));
        return toVoPage(mp);
    }

    public Page<ProfitRecordVo> pagePending(long page, long size) {
        Page<ProfitRecord> mp = new Page<>(page, size);
        profitRecordMapper.selectPage(mp, new LambdaQueryWrapper<ProfitRecord>()
                .eq(ProfitRecord::getStatus, ProfitRecordStatus.PENDING)
                .orderByAsc(ProfitRecord::getSubmitTime));
        return toVoPage(mp);
    }

    /**
     * 直属上级查看下级（申报人）的收益申报列表；{@code status} 为空则不限状态。
     * 列表项仅含申报人手机号与金额/状态等精简字段，详情见 {@link #getAsReferrerDetail}。
     */
    public PageVo<ReferrerProfitListItemVo> pageAsReferrer(Long referrerUserId, ProfitRecordStatus status, long page, long pageSize) {
        long p = Math.max(1L, page);
        long s = Math.min(MAX_PAGE_SIZE, Math.max(1L, pageSize));
        Page<ProfitRecord> mp = new Page<>(p, s);
        LambdaQueryWrapper<ProfitRecord> q = new LambdaQueryWrapper<ProfitRecord>()
                .eq(ProfitRecord::getReferrerUserId, referrerUserId);
        if (status != null) {
            q.eq(ProfitRecord::getStatus, status);
        }
        q.orderByDesc(ProfitRecord::getSubmitTime);
        profitRecordMapper.selectPage(mp, q);
        List<ProfitRecord> rows = mp.getRecords();
        Map<Long, String> mobileByUserId = mobileByUserIdForProfitRows(rows);
        return PageVo.<ReferrerProfitListItemVo>builder()
                .records(rows.stream()
                        .map(pr -> toReferrerListItem(pr, mobileByUserId.get(pr.getUserId())))
                        .toList())
                .total(mp.getTotal())
                .page(mp.getCurrent())
                .pageSize(mp.getSize())
                .build();
    }

    private Map<Long, String> mobileByUserIdForProfitRows(List<ProfitRecord> rows) {
        List<Long> userIds = rows.stream()
                .map(ProfitRecord::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getMobile, (a, b) -> a));
    }

    private ReferrerProfitListItemVo toReferrerListItem(ProfitRecord pr, String userMobile) {
        return ReferrerProfitListItemVo.builder()
                .id(pr.getId())
                .recordNo(pr.getRecordNo())
                .submitTime(pr.getSubmitTime())
                .userMobile(userMobile)
                .profitAmount(pr.getProfitAmount())
                .commissionRate(pr.getCommissionRate())
                .dueShareAmount(pr.getCommissionAmount())
                .netAmount(pr.getNetAmount())
                .status(pr.getStatus())
                .build();
    }

    /**
     * 直属上级查看单条申报详情；非本人名下申报返回 null（由调用方转 404）。
     * 返回体含申报人昵称、策略名称，不含 userId / referrerUserId / strategyId。
     */
    public ReferrerProfitRecordDetailVo getAsReferrerDetail(Long profitRecordId, Long referrerUserId) {
        ProfitRecord pr = profitRecordMapper.selectById(profitRecordId);
        if (pr == null) {
            return null;
        }
        if (!referrerUserId.equals(pr.getReferrerUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该收益申报");
        }
        SysUser submitter = pr.getUserId() != null ? sysUserMapper.selectById(pr.getUserId()) : null;
        String userNickname = submitter != null ? submitter.getNickname() : null;
        String strategyName = null;
        if (pr.getStrategyId() != null) {
            CommissionStrategy st = commissionStrategyMapper.selectById(pr.getStrategyId());
            if (st != null) {
                strategyName = st.getStrategyName();
            }
        }
        return ReferrerProfitRecordDetailVo.builder()
                .id(pr.getId())
                .recordNo(pr.getRecordNo())
                .userNickname(userNickname)
                .strategyName(strategyName)
                .profitAmount(pr.getProfitAmount())
                .commissionRate(pr.getCommissionRate())
                .commissionAmount(pr.getCommissionAmount())
                .netAmount(pr.getNetAmount())
                .profitScreenshotUrl(pr.getProfitScreenshotUrl())
                .transferScreenshotUrl(pr.getTransferScreenshotUrl())
                .status(pr.getStatus())
                .submitTime(pr.getSubmitTime())
                .auditTime(pr.getAuditTime())
                .auditBy(pr.getAuditBy())
                .auditRemark(pr.getAuditRemark())
                .build();
    }

    private Page<ProfitRecordVo> toVoPage(Page<ProfitRecord> mp) {
        Page<ProfitRecordVo> out = new Page<>(mp.getCurrent(), mp.getSize(), mp.getTotal());
        out.setRecords(mp.getRecords().stream().map(this::toVo).toList());
        return out;
    }

    private ProfitRecordVo toVo(ProfitRecord pr) {
        return ProfitRecordVo.builder()
                .id(pr.getId())
                .recordNo(pr.getRecordNo())
                .userId(pr.getUserId())
                .referrerUserId(pr.getReferrerUserId())
                .strategyId(pr.getStrategyId())
                .profitAmount(pr.getProfitAmount())
                .commissionRate(pr.getCommissionRate())
                .commissionAmount(pr.getCommissionAmount())
                .netAmount(pr.getNetAmount())
                .profitScreenshotUrl(pr.getProfitScreenshotUrl())
                .transferScreenshotUrl(pr.getTransferScreenshotUrl())
                .status(pr.getStatus())
                .submitTime(pr.getSubmitTime())
                .auditTime(pr.getAuditTime())
                .auditBy(pr.getAuditBy())
                .auditRemark(pr.getAuditRemark())
                .build();
    }

    private String nextRecordNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PR" + ts + rnd;
    }
}

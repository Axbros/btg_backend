package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.ProfitAttachment;
import com.btg.commission.entity.ProfitDistribution;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.ProfitAttachmentFileType;
import com.btg.commission.enums.ProfitReportStatus;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.ProfitAttachmentMapper;
import com.btg.commission.mapper.ProfitDistributionMapper;
import com.btg.commission.mapper.ProfitReportMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.ProfitDistributionVo;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfitReportService {

    private final ProfitReportMapper profitReportMapper;
    private final ProfitDistributionMapper profitDistributionMapper;
    private final ProfitAttachmentMapper profitAttachmentMapper;
    private final BtgUserMapper btgUserMapper;
    private final ProfitDistributionService profitDistributionService;
    private final SettlementOrderMapper settlementOrderMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public Long submit(
            Long userId,
            BigDecimal profitAmount,
            String profitScreenshotUrl,
            String transferToParentScreenshotUrl) {
        if (!StringUtils.hasText(profitScreenshotUrl) || !StringUtils.hasText(transferToParentScreenshotUrl)) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传收益截图与给直属上级的转账截图");
        }
        if (replenishmentApplyMapper.existsOpenByUserId(userId)) {
            throw new BizException(ResultCode.CONFLICT, "存在未结清补仓申请，请先完成归仓");
        }
        BtgUser self = btgUserMapper.selectById(userId);
        if (self == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long refId = self.getReferrerUserId();
        if (refId == null || refId == 0L) {
            throw new BizException(ResultCode.CONFLICT, "根用户不能提交利润上报");
        }
        ProfitDistributionService.BuiltChain chain = profitDistributionService.buildChainOrThrow(userId);
        BigDecimal p = MoneyUtil.money(profitAmount);

        ProfitReport report = new ProfitReport();
        report.setReportNo(nextReportNo());
        report.setReportUserId(userId);
        report.setDirectParentUserId(refId);
        report.setProfitAmount(p);
        report.setStatus(ProfitReportStatus.PENDING_DIRECT_REVIEW);
        report.setSubmitTime(LocalDateTime.now());
        profitReportMapper.insert(report);

        saveAttachment(report.getId(), ProfitAttachmentFileType.PROFIT, profitScreenshotUrl.trim());
        saveAttachment(report.getId(), ProfitAttachmentFileType.TRANSFER, transferToParentScreenshotUrl.trim());

        profitDistributionService.persistDistributionsAndSettlements(
                report.getId(),
                p,
                chain,
                transferToParentScreenshotUrl.trim());

        auditLogService.log(AuditBusinessType.PROFIT_REPORT, report.getId(), AuditAction.SUBMIT, userId, null);
        return report.getId();
    }

    private void saveAttachment(Long reportId, ProfitAttachmentFileType type, String url) {
        ProfitAttachment a = new ProfitAttachment();
        a.setReportId(reportId);
        a.setFileType(type.getCode());
        a.setFileUrl(url);
        profitAttachmentMapper.insert(a);
    }

    public Page<ProfitReport> pageMine(Long userId, long page, long size) {
        Page<ProfitReport> p = new Page<>(page, size);
        return profitReportMapper.selectPage(p, new LambdaQueryWrapper<ProfitReport>()
                .eq(ProfitReport::getReportUserId, userId)
                .orderByDesc(ProfitReport::getSubmitTime));
    }

    public ProfitReport getById(Long id) {
        return profitReportMapper.selectById(id);
    }

    public ProfitReport getReportForViewer(Long reportId, Long viewerUserId) {
        ProfitReport r = profitReportMapper.selectById(reportId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "利润上报不存在");
        }
        if (!viewerCanAccessReport(viewerUserId, r)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该利润单");
        }
        return r;
    }

    public List<ProfitDistributionVo> listDistributionsForReport(Long viewerUserId, Long reportId) {
        ProfitReport r = profitReportMapper.selectById(reportId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "利润上报不存在");
        }
        if (!viewerCanAccessReport(viewerUserId, r)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该分润明细");
        }
        List<ProfitDistribution> list = profitDistributionMapper.selectList(new LambdaQueryWrapper<ProfitDistribution>()
                .eq(ProfitDistribution::getReportId, reportId)
                .orderByAsc(ProfitDistribution::getLevelNo));
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        boolean root = viewer != null && Boolean.TRUE.equals(viewer.getIsRoot());
        if (!root || list.isEmpty()) {
            return list.stream().map(d -> toProfitDistributionVo(d, null)).collect(Collectors.toList());
        }
        Set<Long> beneficiaryIds = list.stream()
                .map(ProfitDistribution::getBeneficiaryUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, BtgUser> userById = new HashMap<>();
        for (Long uid : beneficiaryIds) {
            BtgUser bu = btgUserMapper.selectById(uid);
            if (bu != null) {
                userById.put(uid, bu);
            }
        }
        return list.stream()
                .map(d -> toProfitDistributionVo(d, beneficiaryDisplayLabel(userById.get(d.getBeneficiaryUserId()))))
                .collect(Collectors.toList());
    }

    private static ProfitDistributionVo toProfitDistributionVo(ProfitDistribution d, String beneficiaryDisplayName) {
        return ProfitDistributionVo.builder()
                .id(d.getId())
                .reportId(d.getReportId())
                .beneficiaryUserId(d.getBeneficiaryUserId())
                .levelNo(d.getLevelNo())
                .upperRatio(d.getUpperRatio())
                .lowerRatio(d.getLowerRatio())
                .incomeAmount(d.getIncomeAmount())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .beneficiaryDisplayName(beneficiaryDisplayName)
                .build();
    }

    private static String beneficiaryDisplayLabel(BtgUser u) {
        if (u == null) {
            return null;
        }
        if (StringUtils.hasText(u.getNickname())) {
            return u.getNickname().trim();
        }
        if (StringUtils.hasText(u.getMobile())) {
            return u.getMobile().trim();
        }
        return null;
    }

    public List<ProfitDistribution> listDistributionsForMine(Long viewerUserId) {
        return profitDistributionMapper.selectList(new LambdaQueryWrapper<ProfitDistribution>()
                .eq(ProfitDistribution::getBeneficiaryUserId, viewerUserId)
                .orderByDesc(ProfitDistribution::getId));
    }

    public boolean viewerCanAccessReport(Long viewerUserId, ProfitReport r) {
        if (viewerUserId.equals(r.getReportUserId()) || viewerUserId.equals(r.getDirectParentUserId())) {
            return true;
        }
        Long n = profitDistributionMapper.selectCount(new LambdaQueryWrapper<ProfitDistribution>()
                .eq(ProfitDistribution::getReportId, r.getId())
                .eq(ProfitDistribution::getBeneficiaryUserId, viewerUserId));
        if (n != null && n > 0) {
            return true;
        }
        Long s = settlementOrderMapper.selectCount(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, r.getId())
                .and(w -> w.eq(SettlementOrder::getFromUserId, viewerUserId)
                        .or()
                        .eq(SettlementOrder::getToUserId, viewerUserId)));
        return s != null && s > 0;
    }

    @Data
    @Builder
    public static class PendingReviewBundle {
        private List<ProfitReport> profitReports;
        private List<SettlementOrder> settlementOrders;
    }

    public PendingReviewBundle pendingReviewForDirectSupervisor(Long userId) {
        List<ProfitReport> reports = profitReportMapper.selectList(new LambdaQueryWrapper<ProfitReport>()
                .eq(ProfitReport::getDirectParentUserId, userId)
                .eq(ProfitReport::getStatus, ProfitReportStatus.PENDING_DIRECT_REVIEW)
                .orderByDesc(ProfitReport::getSubmitTime));
        List<SettlementOrder> settlements = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_REVIEW)
                .orderByDesc(SettlementOrder::getId));
        return PendingReviewBundle.builder()
                .profitReports(reports)
                .settlementOrders(settlements)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectByDirectParent(Long reportId, Long parentUserId, String remark) {
        ProfitReport r = profitReportMapper.selectById(reportId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "利润上报不存在");
        }
        if (!r.getDirectParentUserId().equals(parentUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅直属上级可拒绝该利润单");
        }
        if (r.getStatus() != ProfitReportStatus.PENDING_DIRECT_REVIEW) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
        }
        r.setStatus(ProfitReportStatus.REJECTED);
        r.setAuditBy(parentUserId);
        r.setAuditTime(LocalDateTime.now());
        r.setAuditRemark(remark);
        profitReportMapper.updateById(r);
        auditLogService.log(AuditBusinessType.PROFIT_REPORT, r.getId(), AuditAction.REJECT, parentUserId, remark);

        List<SettlementOrder> orders = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, reportId));
        for (SettlementOrder s : orders) {
            if (s.getStatus() == SettlementOrderStatus.APPROVED) {
                continue;
            }
            settlementOrderMapper.update(null, new LambdaUpdateWrapper<SettlementOrder>()
                    .set(SettlementOrder::getStatus, SettlementOrderStatus.REJECTED)
                    .set(SettlementOrder::getAuditBy, parentUserId)
                    .set(SettlementOrder::getAuditTime, LocalDateTime.now())
                    .set(SettlementOrder::getAuditRemark, "利润单被上级拒绝")
                    .eq(SettlementOrder::getId, s.getId()));
        }
    }

    private static String nextReportNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PR" + ts + rnd;
    }
}

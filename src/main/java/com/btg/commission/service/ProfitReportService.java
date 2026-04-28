package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.ProfitReportResubmitRequest;
import com.btg.commission.entity.BtgBusinessFlowLog;
import com.btg.commission.entity.ProfitAttachment;
import com.btg.commission.entity.ProfitDistribution;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.entity.UserProfitConfig;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;
import com.btg.commission.enums.CommissionModeEnum;
import com.btg.commission.enums.ProfitAttachmentFileType;
import com.btg.commission.enums.ProfitReportStatus;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.ProfitAttachmentMapper;
import com.btg.commission.mapper.ProfitDistributionMapper;
import com.btg.commission.mapper.ProfitReportMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.util.FlowLogViewUtil;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.util.ProfitFlowScope;
import com.btg.commission.vo.ProfitDistributionVo;
import com.btg.commission.vo.ProfitReportDetailVO;
import com.btg.commission.vo.ProfitReportMineBriefVO;
import com.btg.commission.vo.ProfitReportPendingReviewItemVO;
import com.btg.commission.vo.SevenDayProfitItemVO;
import com.btg.commission.vo.flow.BusinessFlowNodeVO;
import com.btg.commission.vo.flow.ProfitReportFlowDetailVO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    /** 周一 … 周日，下标 0=周一 与 {@link java.time.DayOfWeek#MONDAY} getValue 1 对齐 */
    private static final String[] WEEKDAY_CN_SHORT = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };

    private final UserService userService;
    private final ProfitReportMapper profitReportMapper;
    private final ProfitDistributionMapper profitDistributionMapper;
    private final ProfitAttachmentMapper profitAttachmentMapper;
    private final BtgUserMapper btgUserMapper;
    private final ProfitDistributionService profitDistributionService;
    private final SettlementOrderMapper settlementOrderMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final AuditLogService auditLogService;
    private final BusinessFlowLogService businessFlowLogService;
    private final UserQualificationGateService userQualificationGateService;
    private final UserProfitConfigService userProfitConfigService;

    @Transactional(rollbackFor = Exception.class)
    public Long submit(
            Long userId,
            BigDecimal profitAmount,
            String profitScreenshotUrl,
            String transferToParentScreenshotUrl) {
        userQualificationGateService.requireApprovedForFormalBusiness(userId);
        if (!StringUtils.hasText(profitScreenshotUrl) || !StringUtils.hasText(transferToParentScreenshotUrl)) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传收益截图与给直属上级的转账截图");
        }
        if (replenishmentApplyMapper.existsBlockingReplenishmentForUser(userId)) {
            throw new BizException(ResultCode.CONFLICT, "存在未完成的补仓申请，请先完成补仓流程后再上报利润");
        }
        BtgUser self = btgUserMapper.selectById(userId);
        if (self == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long refId = self.getReferrerUserId();
        if (refId == null || refId == 0L) {
            throw new BizException(ResultCode.CONFLICT, "根用户不能提交利润上报");
        }
        assertAtMostOneNewReportPerCalendarDay(userId);
        UserProfitConfig directCfg = userProfitConfigService.findActiveForUserAsChild(userId);
        CommissionModeEnum commissionMode = resolveCommissionModeFromDirectConfig(directCfg);
        ProfitDistributionService.BuiltChain chain = profitDistributionService.buildChainOrThrow(userId, commissionMode);
        BigDecimal p = MoneyUtil.money(profitAmount);

        ProfitReport report = new ProfitReport();
        report.setReportNo(nextReportNo());
        report.setReportUserId(userId);
        report.setDirectParentUserId(refId);
        report.setProfitAmount(p);
        report.setCommissionMode(commissionMode.name());
        report.setStatus(ProfitReportStatus.PENDING_DIRECT_REVIEW);
        report.setSubmitTime(LocalDateTime.now());
        report.setSubmitVersion(1);
        report.setCurrentHandlerUserId(refId);
        report.setReturnedToUser(false);
        report.setFlowStatus(ProfitReportStatus.PENDING_DIRECT_REVIEW.name());
        report.setCurrentStepStatus(ProfitReportStatus.PENDING_DIRECT_REVIEW.name());
        profitReportMapper.insert(report);

        saveAttachment(report.getId(), ProfitAttachmentFileType.PROFIT, profitScreenshotUrl.trim());
        saveAttachment(report.getId(), ProfitAttachmentFileType.TRANSFER, transferToParentScreenshotUrl.trim());

        profitDistributionService.persistDistributionsAndSettlements(
                report.getId(),
                p,
                chain,
                transferToParentScreenshotUrl.trim(),
                commissionMode.name());

        auditLogService.log(AuditBusinessType.PROFIT_REPORT, report.getId(), AuditAction.SUBMIT, userId, null);
        businessFlowLogService.append(
                BusinessFlowType.PROFIT_REPORT,
                report.getId(),
                report.getId(),
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.SUBMIT,
                ProfitReportStatus.PENDING_DIRECT_REVIEW.name(),
                1,
                null,
                userId);
        return report.getId();
    }

    private void saveAttachment(Long reportId, ProfitAttachmentFileType type, String url) {
        ProfitAttachment a = new ProfitAttachment();
        a.setReportId(reportId);
        a.setFileType(type.getCode());
        a.setFileUrl(url);
        profitAttachmentMapper.insert(a);
    }

    private void upsertAttachmentUrl(Long reportId, ProfitAttachmentFileType type, String url) {
        Long cnt = profitAttachmentMapper.selectCount(new LambdaQueryWrapper<ProfitAttachment>()
                .eq(ProfitAttachment::getReportId, reportId)
                .eq(ProfitAttachment::getFileType, type.getCode()));
        if (cnt != null && cnt > 0) {
            profitAttachmentMapper.update(null, new LambdaUpdateWrapper<ProfitAttachment>()
                    .set(ProfitAttachment::getFileUrl, url)
                    .eq(ProfitAttachment::getReportId, reportId)
                    .eq(ProfitAttachment::getFileType, type.getCode()));
        } else {
            saveAttachment(reportId, type, url);
        }
    }


    /**
     * 当前用户近 7 个自然日（含今日，按上海时区划日）的每日已上报利润，缺日补 0，供图表使用。
     */
    public List<SevenDayProfitItemVO> listMineSevenDayProfit(Long userId) {
        LocalDate end = LocalDate.now(SHANGHAI);
        LocalDate start = end.minusDays(6);
        LocalDateTime from = start.atStartOfDay(SHANGHAI).toLocalDateTime();
        LocalDateTime toExclusive = end.plusDays(1).atStartOfDay(SHANGHAI).toLocalDateTime();
        List<ProfitReport> rows = profitReportMapper.selectList(new LambdaQueryWrapper<ProfitReport>()
                .eq(ProfitReport::getReportUserId, userId)
                .ge(ProfitReport::getSubmitTime, from)
                .lt(ProfitReport::getSubmitTime, toExclusive)
                .select(ProfitReport::getSubmitTime, ProfitReport::getProfitAmount));
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (ProfitReport r : rows) {
            if (r.getSubmitTime() == null) {
                continue;
            }
            LocalDate d = r.getSubmitTime().atZone(SHANGHAI).toLocalDate();
            BigDecimal add = MoneyUtil.money(r.getProfitAmount());
            byDay.merge(d, add, BigDecimal::add);
        }
        DateTimeFormatter keyFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<SevenDayProfitItemVO> out = new ArrayList<>(7);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            int w = d.getDayOfWeek().getValue();
            out.add(SevenDayProfitItemVO.builder()
                    .date(WEEKDAY_CN_SHORT[w - 1])
                    .dateKey(d.format(keyFmt))
                    .profit(MoneyUtil.money(byDay.getOrDefault(d, BigDecimal.ZERO)))
                    .build());
        }
        return out;
    }

    public Page<ProfitReportMineBriefVO> pageMine(Long userId, long page, long size) {
        Page<ProfitReport> p = new Page<>(page, size);
        Page<ProfitReport> raw = profitReportMapper.selectPage(p, new LambdaQueryWrapper<ProfitReport>()
                .select(ProfitReport::getId, ProfitReport::getReportNo, ProfitReport::getStatus,
                        ProfitReport::getSubmitTime, ProfitReport::getProfitAmount, ProfitReport::getCommissionMode)
                .eq(ProfitReport::getReportUserId, userId)
                .orderByDesc(ProfitReport::getSubmitTime));
        Page<ProfitReportMineBriefVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(this::toMineBriefVo).toList());
        return out;
    }

    private ProfitReportMineBriefVO toMineBriefVo(ProfitReport e) {
        return ProfitReportMineBriefVO.builder()
                .id(e.getId())
                .reportNo(e.getReportNo())
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .submitTime(e.getSubmitTime())
                .profitAmount(MoneyUtil.money(e.getProfitAmount()))
                .commissionMode(e.getCommissionMode())
                .commissionModeDesc(CommissionModeEnum.descriptionOrNull(e.getCommissionMode()))
                .build();
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

    public ProfitReportDetailVO getReportDetailForViewer(Long reportId, Long viewerUserId) {
        ProfitReport r = getReportForViewer(reportId, viewerUserId);
        return ProfitReportDetailVO.builder()
                .report(r)
                .commissionModeDesc(CommissionModeEnum.descriptionOrNull(r.getCommissionMode()))
                .build();
    }

    /**
     * 从直属上级为上报人生效的配置读取分润模式（创建利润单时快照）。
     * 旧数据未写入 {@code commission_mode} 时与仅维护 {@code child_profit_ratio} 时期语义一致，按兜底（GUARANTEE）。
     */
    private static CommissionModeEnum resolveCommissionModeFromDirectConfig(UserProfitConfig directCfg) {
        CommissionModeEnum m = CommissionModeEnum.fromCode(directCfg.getCommissionMode());
        return m != null ? m : CommissionModeEnum.GUARANTEE;
    }

    public ProfitReportFlowDetailVO flowDetail(Long viewerUserId, Long reportId) {
        ProfitReport r = getReportForViewer(reportId, viewerUserId);
        List<SettlementOrder> allOrders = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, reportId)
                .orderByAsc(SettlementOrder::getLevelNo));
        Set<Long> visible = ProfitFlowScope.visibleUserIds(r, allOrders, viewerUserId, btgUserMapper, userService);
        List<BtgBusinessFlowLog> logs = businessFlowLogService.listForBusiness(BusinessFlowType.PROFIT_REPORT, reportId);
        List<BtgBusinessFlowLog> scopedLogs = ProfitFlowScope.filterFlowLogs(logs, visible);
        List<BusinessFlowNodeVO> nodes = FlowLogViewUtil.toFlowNodes(scopedLogs, id -> btgUserMapper.selectById(id));
        BtgUser applicant = btgUserMapper.selectById(r.getReportUserId());
        BtgUser handler = r.getCurrentHandlerUserId() == null ? null : btgUserMapper.selectById(r.getCurrentHandlerUserId());
        BtgUser lastRejecter = r.getLastRejectBy() == null ? null : btgUserMapper.selectById(r.getLastRejectBy());
        return ProfitReportFlowDetailVO.builder()
                .report(r)
                .commissionMode(r.getCommissionMode())
                .commissionModeDesc(CommissionModeEnum.descriptionOrNull(r.getCommissionMode()))
                .applicantUserId(r.getReportUserId())
                .applicantNickname(applicant != null ? applicant.getNickname() : null)
                .applicantMobile(applicant != null ? applicant.getMobile() : null)
                .currentHandlerUserId(r.getCurrentHandlerUserId())
                .currentHandlerNickname(handler != null ? handler.getNickname() : null)
                .currentStatus(r.getStatus())
                .returnedToApplicant(Boolean.TRUE.equals(r.getReturnedToUser()))
                .submitVersion(r.getSubmitVersion() == null ? 1 : r.getSubmitVersion())
                .lastRejectReason(r.getLastRejectReason())
                .lastRejectByNickname(lastRejecter != null ? lastRejecter.getNickname() : null)
                .nodes(nodes)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long userId, Long reportId, ProfitReportResubmitRequest req) {
        userQualificationGateService.requireApprovedForFormalBusiness(userId);
        ProfitReport r = profitReportMapper.selectById(reportId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND, "利润上报不存在");
        }
        if (!userId.equals(r.getReportUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申报人可重新提交；结算被上级拒绝时请通过对应结算单重新上传划转凭证");
        }
        if (r.getStatus() != ProfitReportStatus.RETURNED_TO_APPLICANT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可重新提交");
        }
        if (replenishmentApplyMapper.existsBlockingReplenishmentForUser(r.getReportUserId())) {
            throw new BizException(ResultCode.CONFLICT, "存在未完成的补仓申请，请先完成补仓流程后再上报利润");
        }
        BigDecimal p = MoneyUtil.money(req.getProfitAmount());
        CommissionModeEnum modeSnap = CommissionModeEnum.fromCode(r.getCommissionMode());
        if (modeSnap == null) {
            UserProfitConfig directCfg = userProfitConfigService.findActiveForUserAsChild(r.getReportUserId());
            modeSnap = resolveCommissionModeFromDirectConfig(directCfg);
        }
        ProfitDistributionService.BuiltChain chain = profitDistributionService.buildChainOrThrow(r.getReportUserId(), modeSnap);

        profitDistributionService.softDeleteDistributionsAndSettlementsByReportId(reportId);

        upsertAttachmentUrl(reportId, ProfitAttachmentFileType.PROFIT, req.getProfitScreenshotUrl().trim());
        upsertAttachmentUrl(reportId, ProfitAttachmentFileType.TRANSFER, req.getTransferScreenshotUrl().trim());

        profitDistributionService.persistDistributionsAndSettlements(
                reportId,
                p,
                chain,
                req.getTransferScreenshotUrl().trim(),
                modeSnap.name());

        int nextVer = (r.getSubmitVersion() == null ? 1 : r.getSubmitVersion()) + 1;
        ProfitReport patch = new ProfitReport();
        patch.setId(reportId);
        patch.setProfitAmount(p);
        if (!StringUtils.hasText(r.getCommissionMode())) {
            patch.setCommissionMode(modeSnap.name());
        }
        patch.setStatus(ProfitReportStatus.PENDING_DIRECT_REVIEW);
        patch.setSubmitTime(LocalDateTime.now());
        patch.setSubmitVersion(nextVer);
        patch.setCurrentHandlerUserId(r.getDirectParentUserId());
        patch.setReturnedToUser(false);
        patch.setFlowStatus(ProfitReportStatus.PENDING_DIRECT_REVIEW.name());
        patch.setCurrentStepStatus(ProfitReportStatus.PENDING_DIRECT_REVIEW.name());
        patch.setLastRejectReason(null);
        patch.setLastRejectTime(null);
        patch.setLastRejectBy(null);
        patch.setAuditRemark(null);
        patch.setAuditBy(null);
        patch.setAuditTime(null);
        profitReportMapper.updateById(patch);

        auditLogService.log(AuditBusinessType.PROFIT_REPORT, reportId, AuditAction.SUBMIT, userId, "resubmit");
        businessFlowLogService.append(
                BusinessFlowType.PROFIT_REPORT,
                reportId,
                reportId,
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.RESUBMIT,
                ProfitReportStatus.PENDING_DIRECT_REVIEW.name(),
                nextVer,
                null,
                userId);
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
                .commissionMode(d.getCommissionMode())
                .commissionModeDesc(CommissionModeEnum.descriptionOrNull(d.getCommissionMode()))
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
        if (viewerUserId.equals(r.getCurrentHandlerUserId())) {
            return true;
        }
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        if (viewer != null && Boolean.TRUE.equals(viewer.getIsRoot())) {
            return true;
        }
        if (userService.isUpstreamOf(viewerUserId, r.getReportUserId())) {
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
        private List<ProfitReportPendingReviewItemVO> profitReports;
        private List<SettlementOrder> settlementOrders;
    }

    public PendingReviewBundle pendingReviewForDirectSupervisor(Long userId) {
        List<ProfitReport> reports = profitReportMapper.selectList(new LambdaQueryWrapper<ProfitReport>()
                .eq(ProfitReport::getDirectParentUserId, userId)
                .eq(ProfitReport::getStatus, ProfitReportStatus.PENDING_DIRECT_REVIEW)
                .orderByDesc(ProfitReport::getSubmitTime));
        List<ProfitReportPendingReviewItemVO> reportRows = reports.stream().map(this::toPendingReviewItemVo).toList();
        List<SettlementOrder> settlements = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_REVIEW)
                .orderByDesc(SettlementOrder::getId));
        return PendingReviewBundle.builder()
                .profitReports(reportRows)
                .settlementOrders(settlements)
                .build();
    }

    private ProfitReportPendingReviewItemVO toPendingReviewItemVo(ProfitReport e) {
        return ProfitReportPendingReviewItemVO.builder()
                .id(e.getId())
                .reportNo(e.getReportNo())
                .reportUserId(e.getReportUserId())
                .profitAmount(MoneyUtil.money(e.getProfitAmount()))
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .submitTime(e.getSubmitTime())
                .commissionMode(e.getCommissionMode())
                .commissionModeDesc(CommissionModeEnum.descriptionOrNull(e.getCommissionMode()))
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
        LocalDateTime now = LocalDateTime.now();
        ProfitReport patch = new ProfitReport();
        patch.setId(reportId);
        patch.setStatus(ProfitReportStatus.RETURNED_TO_APPLICANT);
        patch.setAuditBy(parentUserId);
        patch.setAuditTime(now);
        patch.setAuditRemark(remark);
        patch.setCurrentHandlerUserId(r.getReportUserId());
        patch.setReturnedToUser(true);
        patch.setFlowStatus(ProfitReportStatus.RETURNED_TO_APPLICANT.name());
        patch.setCurrentStepStatus(ProfitReportStatus.RETURNED_TO_APPLICANT.name());
        patch.setLastRejectReason(StringUtils.hasText(remark) ? remark.trim() : null);
        patch.setLastRejectTime(now);
        patch.setLastRejectBy(parentUserId);
        profitReportMapper.updateById(patch);

        auditLogService.log(AuditBusinessType.PROFIT_REPORT, r.getId(), AuditAction.REJECT, parentUserId, remark);
        businessFlowLogService.append(
                BusinessFlowType.PROFIT_REPORT,
                reportId,
                reportId,
                r.getReportUserId(),
                FlowNodeRole.DIRECT_PARENT,
                FlowAction.RETURN_TO_APPLICANT,
                ProfitReportStatus.RETURNED_TO_APPLICANT.name(),
                r.getSubmitVersion() == null ? 1 : r.getSubmitVersion(),
                remark,
                parentUserId);

        List<SettlementOrder> orders = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, reportId));
        for (SettlementOrder s : orders) {
            if (s.getStatus() == SettlementOrderStatus.APPROVED) {
                continue;
            }
            settlementOrderMapper.update(null, new LambdaUpdateWrapper<SettlementOrder>()
                    .set(SettlementOrder::getStatus, SettlementOrderStatus.REJECTED)
                    .set(SettlementOrder::getAuditBy, parentUserId)
                    .set(SettlementOrder::getAuditTime, now)
                    .set(SettlementOrder::getAuditRemark, "利润单被上级拒绝")
                    .eq(SettlementOrder::getId, s.getId()));
        }
    }

    // 上海自然日：新建上报每日至多一条；退回后修改走 resubmit。
    private void assertAtMostOneNewReportPerCalendarDay(Long userId) {
        LocalDate today = LocalDate.now(SHANGHAI);
        LocalDateTime start = today.atStartOfDay(SHANGHAI).toLocalDateTime();
        LocalDateTime end = today.plusDays(1).atStartOfDay(SHANGHAI).toLocalDateTime();
        Long cnt = profitReportMapper.selectCount(new LambdaQueryWrapper<ProfitReport>()
                .eq(ProfitReport::getReportUserId, userId)
                .ge(ProfitReport::getSubmitTime, start)
                .lt(ProfitReport::getSubmitTime, end));
        if (cnt != null && cnt > 0) {
            throw new BizException(ResultCode.CONFLICT, "每个自然日只能上报一次利润");
        }
    }

    private static String nextReportNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PR" + ts + rnd;
    }
}

package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.ProfitAttachment;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.entity.UserProfitConfig;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;
import com.btg.commission.enums.CommissionModeEnum;
import com.btg.commission.enums.ProfitReportStatus;
import com.btg.commission.enums.ReminderTodoTypeEnum;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.enums.UserProfitConfigStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.ProfitAttachmentMapper;
import com.btg.commission.mapper.ProfitReportMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.mapper.UserProfitConfigMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.vo.SettlementOrderDetailVo;
import com.btg.commission.vo.SettlementOrderListItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettlementOrderService {

    private final SettlementOrderMapper settlementOrderMapper;
    private final ProfitReportMapper profitReportMapper;
    private final BtgUserMapper btgUserMapper;
    private final ProfitAttachmentMapper profitAttachmentMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserProfitConfigMapper userProfitConfigMapper;
    private final AuditLogService auditLogService;
    private final BusinessFlowLogService businessFlowLogService;
    private final ProfitReportService profitReportService;
    private final TodoReminderService todoReminderService;

    public List<SettlementOrder> listMinePayables(Long userId) {
        return settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId)
                .in(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_SUBMIT, SettlementOrderStatus.PENDING_REVIEW)
                .orderByAsc(SettlementOrder::getRootReportId)
                .orderByAsc(SettlementOrder::getLevelNo));
    }

    /**
     * 与 {@link #listMinePayables(Long)} 相同口径：本人为付款人且待提交凭证或待上级审核
     */
    public long countMinePayables(Long userId) {
        Long c = settlementOrderMapper.selectCount(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_SUBMIT));
        return c == null ? 0L : c;
    }

    public List<SettlementOrder> listPendingReviewForMe(Long userId) {
        return settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_REVIEW)
                .orderByAsc(SettlementOrder::getRootReportId)
                .orderByAsc(SettlementOrder::getLevelNo));
    }

    /**
     * @param status 结算单状态码 1～5，与 {@link SettlementOrderStatus} 一致；null 时不限状态（本人为付款人的全部结算单）
     */
    public Page<SettlementOrderListItemVo> pageMinePayables(Long userId, long page, long size, Integer status) {
        Page<SettlementOrder> p = new Page<>(page, size);
        LambdaQueryWrapper<SettlementOrder> q = new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId);
        if (status != null) {
            SettlementOrderStatus s = SettlementOrderStatus.fromCode(status);
            if (s == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "status 须为 1～5 或省略");
            }
            q.eq(SettlementOrder::getStatus, s);
        }
        q.orderByDesc(SettlementOrder::getId);
        Page<SettlementOrder> raw = settlementOrderMapper.selectPage(p, q);
        return toListItemPage(raw);
    }

    /**
     * 路径参数为 {@code root_report_id}，且仅返回当前用户作为付款人（{@code from_user_id}）的结算单行。
     */
    public SettlementOrderDetailVo getDetailByRootReportForPayer(Long rootReportId, Long viewerUserId) {
        SettlementOrder o = settlementOrderMapper.selectOne(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, rootReportId)
                .eq(SettlementOrder::getFromUserId, viewerUserId)
                .last("LIMIT 1"));
        if (o == null) {
            throw new BizException(ResultCode.NOT_FOUND, "结算单不存在");
        }
        return buildSettlementDetailVo(o);
    }

    /**
     * 按结算单主键查询；当前用户须为付款人或收款人（待审核上级等场景）。
     */
    public SettlementOrderDetailVo getDetailBySettlementIdForParty(Long settlementId, Long viewerUserId) {
        SettlementOrder o = settlementOrderMapper.selectById(settlementId);
        if (o == null) {
            throw new BizException(ResultCode.NOT_FOUND, "结算单不存在");
        }
        if (!viewerUserId.equals(o.getFromUserId()) && !viewerUserId.equals(o.getToUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该结算单");
        }
        return buildSettlementDetailVo(o);
    }

    private SettlementOrderDetailVo buildSettlementDetailVo(SettlementOrder o) {
        ProfitReport report = profitReportMapper.selectById(o.getRootReportId());
        String reportNo = report != null ? report.getReportNo() : null;
        Long reportUserId = report != null ? report.getReportUserId() : null;
        BigDecimal profitAmount = report != null ? report.getProfitAmount() : null;
        BtgUser reporter = reportUserId != null ? btgUserMapper.selectById(reportUserId) : null;

        BigDecimal parentToChildProfitRatio = null;
        if (o.getToUserId() != null && o.getFromUserId() != null) {
            UserProfitConfig cfg = userProfitConfigMapper.selectOne(new LambdaQueryWrapper<UserProfitConfig>()
                    .eq(UserProfitConfig::getParentUserId, o.getToUserId())
                    .eq(UserProfitConfig::getChildUserId, o.getFromUserId())
                    .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                    .last("LIMIT 1"));
            if (cfg != null) {
                CommissionModeEnum mode = CommissionModeEnum.fromCode(report != null ? report.getCommissionMode() : null);
                if (mode == null) {
                    mode = CommissionModeEnum.GUARANTEE;
                }
                parentToChildProfitRatio = ProfitDistributionService.resolveRatioByModeOrNull(cfg, mode);
            }
        }

        BtgUser from = o.getFromUserId() != null ? btgUserMapper.selectById(o.getFromUserId()) : null;
        BtgUser to = o.getToUserId() != null ? btgUserMapper.selectById(o.getToUserId()) : null;
        String toUserExchangeUid = null;
        if (o.getToUserId() != null) {
            UserProfile toProfile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, o.getToUserId())
                    .last("LIMIT 1"));
            if (toProfile != null && StringUtils.hasText(toProfile.getExchangeUid())) {
                toUserExchangeUid = toProfile.getExchangeUid().trim();
            }
        }

        List<ProfitAttachment> attachments = profitAttachmentMapper.selectList(new LambdaQueryWrapper<ProfitAttachment>()
                .eq(ProfitAttachment::getReportId, o.getRootReportId())
                .orderByAsc(ProfitAttachment::getId));
        String profitScreenshotUrl = null;
        for (ProfitAttachment a : attachments) {
            if (a == null || !StringUtils.hasText(a.getFileUrl())) {
                continue;
            }
            String t = a.getFileType();
            if (t == null || !"PROFIT".equalsIgnoreCase(t)) {
                continue;
            }
            if (profitScreenshotUrl == null) {
                profitScreenshotUrl = a.getFileUrl().trim();
            }
        }

        return SettlementOrderDetailVo.builder()
                .id(o.getId())
                .rootReportId(o.getRootReportId())
                .fromUserId(o.getFromUserId())
                .toUserId(o.getToUserId())
                .levelNo(o.getLevelNo())
                .payAmount(o.getPayAmount())
                .status(o.getStatus())
                .transferScreenshotUrl(o.getTransferScreenshotUrl())
                .profitScreenshotUrl(profitScreenshotUrl)
                .submitTime(o.getSubmitTime())
                .auditTime(o.getAuditTime())
                .auditBy(o.getAuditBy())
                .auditRemark(o.getAuditRemark())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .reportNo(reportNo)
                .reportUserId(reportUserId)
                .reportUserNickname(nicknameOf(reporter))
                .reportUserMobile(mobileOf(reporter))
                .profitAmount(profitAmount)
                .parentToChildProfitRatio(parentToChildProfitRatio)
                .fromUserNickname(nicknameOf(from))
                .fromUserMobile(mobileOf(from))
                .toUserNickname(nicknameOf(to))
                .toUserMobile(mobileOf(to))
                .toUserExchangeUid(toUserExchangeUid)
                .build();
    }

    private static String nicknameOf(BtgUser u) {
        if (u == null || !StringUtils.hasText(u.getNickname())) {
            return null;
        }
        return u.getNickname().trim();
    }

    private static String mobileOf(BtgUser u) {
        if (u == null || !StringUtils.hasText(u.getMobile())) {
            return null;
        }
        return u.getMobile().trim();
    }

    public Page<SettlementOrderListItemVo> pagePendingReview(Long userId, long page, long size) {
        Page<SettlementOrder> p = new Page<>(page, size);
        Page<SettlementOrder> raw = settlementOrderMapper.selectPage(p, new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_REVIEW)
                .orderByDesc(SettlementOrder::getId));
        return toListItemPage(raw);
    }

    public Page<SettlementOrderListItemVo> pageApprovedAsReviewer(Long userId, long page, long size) {
        Page<SettlementOrder> p = new Page<>(page, size);
        Page<SettlementOrder> raw = settlementOrderMapper.selectPage(p, new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.APPROVED)
                .orderByDesc(SettlementOrder::getId));
        return toListItemPage(raw);
    }

    public Page<SettlementOrderListItemVo> pageRejectedAsReviewer(Long userId, long page, long size) {
        Page<SettlementOrder> p = new Page<>(page, size);
        Page<SettlementOrder> raw = settlementOrderMapper.selectPage(p, new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.REJECTED)
                .orderByDesc(SettlementOrder::getId));
        return toListItemPage(raw);
    }

    public Page<SettlementOrderListItemVo> pageAllReviewStatesAsReviewer(Long userId, long page, long size) {
        Page<SettlementOrder> p = new Page<>(page, size);
        Page<SettlementOrder> raw = settlementOrderMapper.selectPage(p, new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .in(SettlementOrder::getStatus,
                        SettlementOrderStatus.PENDING_REVIEW,
                        SettlementOrderStatus.APPROVED,
                        SettlementOrderStatus.REJECTED)
                .orderByDesc(SettlementOrder::getId));
        return toListItemPage(raw);
    }

    private Page<SettlementOrderListItemVo> toListItemPage(Page<SettlementOrder> raw) {
        Page<SettlementOrderListItemVo> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(toListItems(raw.getRecords()));
        return out;
    }

    private List<SettlementOrderListItemVo> toListItems(List<SettlementOrder> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> fromIds = new HashSet<>();
        Set<Long> rootReportIds = new HashSet<>();
        for (SettlementOrder o : records) {
            if (o.getFromUserId() != null) {
                fromIds.add(o.getFromUserId());
            }
            if (o.getRootReportId() != null) {
                rootReportIds.add(o.getRootReportId());
            }
        }
        Map<Long, ProfitReport> reportById = new HashMap<>();
        if (!rootReportIds.isEmpty()) {
            for (ProfitReport r : profitReportMapper.selectList(new LambdaQueryWrapper<ProfitReport>()
                    .in(ProfitReport::getId, rootReportIds))) {
                if (r.getId() != null) {
                    reportById.put(r.getId(), r);
                }
            }
        }
        Set<Long> userIds = new HashSet<>(fromIds);
        for (ProfitReport r : reportById.values()) {
            if (r.getReportUserId() != null) {
                userIds.add(r.getReportUserId());
            }
        }
        Map<Long, BtgUser> byId = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (BtgUser u : btgUserMapper.selectList(new LambdaQueryWrapper<BtgUser>().in(BtgUser::getId, userIds))) {
                byId.put(u.getId(), u);
            }
        }
        List<SettlementOrderListItemVo> list = new ArrayList<>(records.size());
        for (SettlementOrder o : records) {
            ProfitReport profitReport = o.getRootReportId() == null ? null : reportById.get(o.getRootReportId());
            Long reporterId = profitReport == null ? null : profitReport.getReportUserId();
            BtgUser reporter = reporterId == null ? null : byId.get(reporterId);
//            BtgUser from = o.getFromUserId() == null ? null : byId.get(o.getFromUserId());
            list.add(SettlementOrderListItemVo.builder()
                    .id(o.getId())
                    .rootReportId(o.getRootReportId())
                    .reportUserNickname(nicknameOf(reporter))

//                    .fromUserId(o.getFromUserId())
//                    .toUserId(o.getToUserId())
//                    .levelNo(o.getLevelNo())
                    .payAmount(o.getPayAmount())
                    .status(o.getStatus())
//                    .transferScreenshotUrl(o.getTransferScreenshotUrl())
//                    .submitTime(o.getSubmitTime())
//                    .auditTime(o.getAuditTime())
//                    .auditBy(o.getAuditBy())
//                    .auditRemark(o.getAuditRemark())
//                    .createdAt(o.getCreatedAt())
//                    .updatedAt(o.getUpdatedAt())
//                    .deletedAt(o.getDeletedAt())
//                    .fromUserNickname(nicknameOf(from))
//                    .fromUserMobile(mobileOf(from))
                    .build());
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitTransferProof(Long settlementId, Long payerUserId, String transferScreenshotUrl) {
        if (!StringUtils.hasText(transferScreenshotUrl)) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传转账截图 URL");
        }
        SettlementOrder o = settlementOrderMapper.selectById(settlementId);
        if (o == null) {
            throw new BizException(ResultCode.NOT_FOUND, "结算单不存在");
        }
        if (!o.getFromUserId().equals(payerUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅付款人可提交凭证");
        }
        if (o.getStatus() != SettlementOrderStatus.PENDING_SUBMIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可提交");
        }
        o.setTransferScreenshotUrl(transferScreenshotUrl.trim());
        o.setStatus(SettlementOrderStatus.PENDING_REVIEW);
        o.setSubmitTime(LocalDateTime.now());
        settlementOrderMapper.updateById(o);
        auditLogService.log(AuditBusinessType.SETTLEMENT_ORDER, o.getId(), AuditAction.SUBMIT, payerUserId, null);
        todoReminderService.resolveDone(ReminderTodoTypeEnum.SETTLEMENT_PAYABLE, "settlement", o.getId(), payerUserId);
        todoReminderService.upsertOpen(
                ReminderTodoTypeEnum.SETTLEMENT_REVIEW,
                "settlement",
                o.getId(),
                o.getToUserId(),
                SettlementOrderStatus.PENDING_REVIEW.name(),
                o.getSubmitTime());
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long settlementId, Long reviewerUserId, String remark) {
        SettlementOrder o = settlementOrderMapper.selectById(settlementId);
        if (o == null) {
            throw new BizException(ResultCode.NOT_FOUND, "结算单不存在");
        }
        if (!o.getToUserId().equals(reviewerUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅收款上级可审核");
        }
        if (o.getStatus() != SettlementOrderStatus.PENDING_REVIEW) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可审核通过");
        }
        ProfitReport report = profitReportMapper.selectById(o.getRootReportId());
        if (report == null
                || report.getStatus() == ProfitReportStatus.REJECTED
                || report.getStatus() == ProfitReportStatus.RETURNED_TO_APPLICANT) {
            throw new BizException(ResultCode.CONFLICT, "关联利润单不可用");
        }

        o.setStatus(SettlementOrderStatus.APPROVED);
        o.setAuditTime(LocalDateTime.now());
        o.setAuditBy(reviewerUserId);
        o.setAuditRemark(remark);
        settlementOrderMapper.updateById(o);
        auditLogService.log(AuditBusinessType.SETTLEMENT_ORDER, o.getId(), AuditAction.APPROVE, reviewerUserId, remark);
        todoReminderService.resolveDone(ReminderTodoTypeEnum.SETTLEMENT_REVIEW, "settlement", o.getId(), reviewerUserId);
        if (o.getFromUserId() != null) {
            todoReminderService.resolveDone(ReminderTodoTypeEnum.SETTLEMENT_PAYABLE, "settlement", o.getId(), o.getFromUserId());
        }

        if (report.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW) {
            ProfitReport patch = new ProfitReport();
            patch.setId(report.getId());
            patch.setStatus(ProfitReportStatus.IN_SETTLEMENT_CHAIN);
            profitReportMapper.updateById(patch);
            todoReminderService.resolveDone(
                    ReminderTodoTypeEnum.PROFIT_REPORT_REVIEW,
                    "profit_report",
                    report.getId(),
                    report.getDirectParentUserId());
        }

        SettlementOrder next = settlementOrderMapper.selectOne(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, o.getRootReportId())
                .eq(SettlementOrder::getLevelNo, o.getLevelNo() + 1)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.INIT)
                .last("LIMIT 1"));
        if (next != null) {
            next.setStatus(SettlementOrderStatus.PENDING_SUBMIT);
            settlementOrderMapper.updateById(next);
            todoReminderService.upsertOpen(
                    ReminderTodoTypeEnum.SETTLEMENT_PAYABLE,
                    "settlement",
                    next.getId(),
                    next.getFromUserId(),
                    SettlementOrderStatus.PENDING_SUBMIT.name(),
                    next.getUpdatedAt());
        } else {
            ProfitReport done = new ProfitReport();
            done.setId(report.getId());
            done.setStatus(ProfitReportStatus.ALL_COMPLETED);
            profitReportMapper.updateById(done);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long settlementId, Long reviewerUserId, String remark) {
        SettlementOrder o = settlementOrderMapper.selectById(settlementId);
        if (o == null) {
            throw new BizException(ResultCode.NOT_FOUND, "结算单不存在");
        }
        if (!o.getToUserId().equals(reviewerUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅收款上级可拒绝");
        }
        if (o.getStatus() != SettlementOrderStatus.PENDING_REVIEW) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
        }

        ProfitReport reportEarly = profitReportMapper.selectById(o.getRootReportId());
        boolean returnToApplicant = reportEarly != null
                && reportEarly.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW
                && Objects.equals(reportEarly.getReportUserId(), o.getFromUserId())
                && Objects.equals(reportEarly.getDirectParentUserId(), o.getToUserId());
        if (returnToApplicant) {
            auditLogService.log(AuditBusinessType.SETTLEMENT_ORDER, o.getId(), AuditAction.REJECT, reviewerUserId, remark);
            profitReportService.rejectByDirectParent(o.getRootReportId(), reviewerUserId, remark);
            return;
        }

        o.setStatus(SettlementOrderStatus.REJECTED);
        o.setAuditTime(LocalDateTime.now());
        o.setAuditBy(reviewerUserId);
        o.setAuditRemark(remark);
        settlementOrderMapper.updateById(o);
        auditLogService.log(AuditBusinessType.SETTLEMENT_ORDER, o.getId(), AuditAction.REJECT, reviewerUserId, remark);
        todoReminderService.resolveDone(ReminderTodoTypeEnum.SETTLEMENT_REVIEW, "settlement", o.getId(), reviewerUserId);

        List<SettlementOrder> siblings = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, o.getRootReportId()));
        LocalDateTime now = LocalDateTime.now();
        for (SettlementOrder s : siblings) {
            if (s.getId().equals(o.getId())) {
                continue;
            }
            if (s.getStatus() == SettlementOrderStatus.APPROVED || s.getStatus() == SettlementOrderStatus.REJECTED) {
                continue;
            }
            // 尚未激活的后续层级保持 INIT，避免整条链被误标 REJECTED 后无法恢复
            if (s.getStatus() == SettlementOrderStatus.INIT) {
                continue;
            }
            settlementOrderMapper.update(null, new LambdaUpdateWrapper<SettlementOrder>()
                    .set(SettlementOrder::getStatus, SettlementOrderStatus.REJECTED)
                    .set(SettlementOrder::getAuditBy, reviewerUserId)
                    .set(SettlementOrder::getAuditTime, now)
                    .set(SettlementOrder::getAuditRemark, "链上审核拒绝")
                    .eq(SettlementOrder::getId, s.getId()));
            if (s.getToUserId() != null) {
                todoReminderService.resolveDone(ReminderTodoTypeEnum.SETTLEMENT_REVIEW, "settlement", s.getId(), s.getToUserId());
            }
            if (s.getFromUserId() != null) {
                todoReminderService.resolveDone(ReminderTodoTypeEnum.SETTLEMENT_PAYABLE, "settlement", s.getId(), s.getFromUserId());
            }
        }

        ProfitReport report = profitReportMapper.selectById(o.getRootReportId());
        if (report == null) {
            return;
        }
        boolean pendingDirect = report.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW;
        LambdaUpdateWrapper<ProfitReport> reportUw = new LambdaUpdateWrapper<ProfitReport>()
                .set(ProfitReport::getCurrentHandlerUserId, o.getFromUserId())
                .set(ProfitReport::getReturnedToUser, false)
                .set(ProfitReport::getLastRejectReason, StringUtils.hasText(remark) ? remark.trim() : null)
                .set(ProfitReport::getLastRejectTime, now)
                .set(ProfitReport::getLastRejectBy, reviewerUserId)
                .eq(ProfitReport::getId, o.getRootReportId());
        if (pendingDirect) {
            reportUw.set(ProfitReport::getFlowStatus, ProfitReportStatus.PENDING_DIRECT_REVIEW.name())
                    .set(ProfitReport::getCurrentStepStatus, ProfitReportStatus.PENDING_DIRECT_REVIEW.name());
        } else {
            reportUw.set(ProfitReport::getStatus, ProfitReportStatus.IN_SETTLEMENT_CHAIN)
                    .set(ProfitReport::getFlowStatus, ProfitReportStatus.IN_SETTLEMENT_CHAIN.name())
                    .set(ProfitReport::getCurrentStepStatus, ProfitReportStatus.IN_SETTLEMENT_CHAIN.name());
        }
        profitReportMapper.update(null, reportUw);

        // 付款人走结算「提交凭证」流程重新上传，不进入利润单 resubmit
        settlementOrderMapper.update(null, new LambdaUpdateWrapper<SettlementOrder>()
                .set(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_SUBMIT)
                .set(SettlementOrder::getTransferScreenshotUrl, null)
                .set(SettlementOrder::getSubmitTime, null)
                .set(SettlementOrder::getAuditBy, null)
                .set(SettlementOrder::getAuditTime, null)
                .set(SettlementOrder::getAuditRemark, null)
                .eq(SettlementOrder::getId, o.getId()));
        todoReminderService.upsertOpen(
                ReminderTodoTypeEnum.SETTLEMENT_PAYABLE,
                "settlement",
                o.getId(),
                o.getFromUserId(),
                SettlementOrderStatus.PENDING_SUBMIT.name(),
                now);


        // 使用 REJECT 而非 RETURN_TO_APPLICANT：后者在流程展示上会映射为「退回改单」，易误导前端当成根申报人 resubmit；
        // 链上拒单只退回本笔结算的付款人（from_user_id），利润单状态保持待审/结算链，见 profit 字段而非 5 RETURNED。
        businessFlowLogService.append(
                BusinessFlowType.PROFIT_REPORT,
                report.getId(),
                report.getId(),
                o.getFromUserId(),
                FlowNodeRole.UPLINE,
                FlowAction.REJECT,
                pendingDirect
                        ? ProfitReportStatus.PENDING_DIRECT_REVIEW.name()
                        : ProfitReportStatus.IN_SETTLEMENT_CHAIN.name(),
                report.getSubmitVersion() == null ? 1 : report.getSubmitVersion(),
                remark,
                reviewerUserId);
    }
}

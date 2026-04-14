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
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;
import com.btg.commission.enums.ProfitFlowLayerState;
import com.btg.commission.enums.ProfitReportStatus;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.ProfitAttachmentMapper;
import com.btg.commission.mapper.ProfitReportMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.util.ProfitFlowScope;
import com.btg.commission.vo.flow.ProfitFlowLayerSummaryVO;
import com.btg.commission.vo.flow.SettlementScopedProfitFlowVO;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettlementOrderService {

    private final SettlementOrderMapper settlementOrderMapper;
    private final ProfitReportMapper profitReportMapper;
    private final BtgUserMapper btgUserMapper;
    private final ProfitAttachmentMapper profitAttachmentMapper;
    private final UserProfileMapper userProfileMapper;
    private final AuditLogService auditLogService;
    private final BusinessFlowLogService businessFlowLogService;
    private final UserService userService;
    private final ProfitReportService profitReportService;

    public List<SettlementOrder> listMinePayables(Long userId) {
        return settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId)
                .in(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_SUBMIT, SettlementOrderStatus.PENDING_REVIEW)
                .orderByAsc(SettlementOrder::getRootReportId)
                .orderByAsc(SettlementOrder::getLevelNo));
    }

    /** 与 {@link #listMinePayables(Long)} 相同口径：本人为付款人且待提交凭证或待上级审核 */
    public long countMinePayables(Long userId) {
        Long c = settlementOrderMapper.selectCount(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId)
                .in(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_SUBMIT, SettlementOrderStatus.PENDING_REVIEW));
        return c == null ? 0L : c;
    }

    public List<SettlementOrder> listPendingReviewForMe(Long userId) {
        return settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_REVIEW)
                .orderByAsc(SettlementOrder::getRootReportId)
                .orderByAsc(SettlementOrder::getLevelNo));
    }

    public Page<SettlementOrderListItemVo> pageMinePayables(Long userId, long page, long size) {
        Page<SettlementOrder> p = new Page<>(page, size);
        Page<SettlementOrder> raw = settlementOrderMapper.selectPage(p, new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId)
                .in(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_SUBMIT, SettlementOrderStatus.PENDING_REVIEW)
                .orderByDesc(SettlementOrder::getId));
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
        for (SettlementOrder o : records) {
            if (o.getFromUserId() != null) {
                fromIds.add(o.getFromUserId());
            }
        }
        Map<Long, BtgUser> byId = new HashMap<>();
        if (!fromIds.isEmpty()) {
            List<BtgUser> users = btgUserMapper.selectList(new LambdaQueryWrapper<BtgUser>().in(BtgUser::getId, fromIds));
            for (BtgUser u : users) {
                byId.put(u.getId(), u);
            }
        }
        List<SettlementOrderListItemVo> list = new ArrayList<>(records.size());
        for (SettlementOrder o : records) {
            BtgUser from = o.getFromUserId() == null ? null : byId.get(o.getFromUserId());
            list.add(SettlementOrderListItemVo.builder()
                    .id(o.getId())
                    .rootReportId(o.getRootReportId())
                    .fromUserId(o.getFromUserId())
                    .toUserId(o.getToUserId())
                    .levelNo(o.getLevelNo())
                    .payAmount(o.getPayAmount())
                    .status(o.getStatus())
                    .transferScreenshotUrl(o.getTransferScreenshotUrl())
                    .submitTime(o.getSubmitTime())
                    .auditTime(o.getAuditTime())
                    .auditBy(o.getAuditBy())
                    .auditRemark(o.getAuditRemark())
                    .createdAt(o.getCreatedAt())
                    .updatedAt(o.getUpdatedAt())
                    .deletedAt(o.getDeletedAt())
                    .fromUserNickname(nicknameOf(from))
                    .fromUserMobile(mobileOf(from))
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

        if (report.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW) {
            ProfitReport patch = new ProfitReport();
            patch.setId(report.getId());
            patch.setStatus(ProfitReportStatus.IN_SETTLEMENT_CHAIN);
            profitReportMapper.updateById(patch);
        }

        SettlementOrder next = settlementOrderMapper.selectOne(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, o.getRootReportId())
                .eq(SettlementOrder::getLevelNo, o.getLevelNo() + 1)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.INIT)
                .last("LIMIT 1"));
        if (next != null) {
            next.setStatus(SettlementOrderStatus.PENDING_SUBMIT);
            settlementOrderMapper.updateById(next);
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
        o.setStatus(SettlementOrderStatus.REJECTED);
        o.setAuditTime(LocalDateTime.now());
        o.setAuditBy(reviewerUserId);
        o.setAuditRemark(remark);
        settlementOrderMapper.updateById(o);
        auditLogService.log(AuditBusinessType.SETTLEMENT_ORDER, o.getId(), AuditAction.REJECT, reviewerUserId, remark);

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
            settlementOrderMapper.update(null, new LambdaUpdateWrapper<SettlementOrder>()
                    .set(SettlementOrder::getStatus, SettlementOrderStatus.REJECTED)
                    .set(SettlementOrder::getAuditBy, reviewerUserId)
                    .set(SettlementOrder::getAuditTime, now)
                    .set(SettlementOrder::getAuditRemark, "链上审核拒绝")
                    .eq(SettlementOrder::getId, s.getId()));
        }

        ProfitReport report = profitReportMapper.selectById(o.getRootReportId());
        if (report == null) {
            return;
        }
        ProfitReport patch = new ProfitReport();
        patch.setId(o.getRootReportId());
        patch.setStatus(ProfitReportStatus.RETURNED_TO_APPLICANT);
        patch.setCurrentHandlerUserId(report.getReportUserId());
        patch.setReturnedToUser(true);
        patch.setFlowStatus("RETURNED_TO_APPLICANT");
        patch.setCurrentStepStatus("RETURNED_TO_APPLICANT");
        patch.setLastRejectReason(StringUtils.hasText(remark) ? remark.trim() : null);
        patch.setLastRejectTime(now);
        patch.setLastRejectBy(reviewerUserId);
        profitReportMapper.updateById(patch);

        businessFlowLogService.append(
                BusinessFlowType.PROFIT_REPORT,
                report.getId(),
                report.getId(),
                report.getReportUserId(),
                FlowNodeRole.UPLINE,
                FlowAction.RETURN_TO_APPLICANT,
                ProfitReportStatus.RETURNED_TO_APPLICANT.name(),
                report.getSubmitVersion() == null ? 1 : report.getSubmitVersion(),
                remark,
                reviewerUserId);
    }

    /**
     * 与 {@code GET /settlements/{rootReportId}} 同源利润单；任意有权限用户可查，
     * 但流转与结算边按「仅本人及下级子树 ∩ 邀请链」或「申报人→本人路径」裁剪，上级看不到更上层。
     */
    public SettlementScopedProfitFlowVO getScopedProfitFlowByRootReportId(Long rootReportId, Long viewerUserId) {
        ProfitReport report = profitReportMapper.selectById(rootReportId);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "利润上报不存在");
        }
        if (!profitReportService.viewerCanAccessReport(viewerUserId, report)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该利润单流转");
        }
        List<SettlementOrder> allOrders = settlementOrderMapper.selectList(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, rootReportId)
                .orderByAsc(SettlementOrder::getLevelNo));
        Set<Long> u = ProfitFlowScope.visibleUserIds(report, allOrders, viewerUserId, btgUserMapper, userService);
        List<SettlementOrder> scoped = ProfitFlowScope.filterSettlements(allOrders, u);
        List<ProfitFlowLayerSummaryVO> layers = buildProfitFlowLayerSummaries(report, scoped, u);

        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        boolean root = viewer != null && Boolean.TRUE.equals(viewer.getIsRoot());
        String scopeType;
        if (viewerUserId.equals(report.getReportUserId()) || root) {
            scopeType = "FULL";
        } else if (userService.isUpstreamOf(viewerUserId, report.getReportUserId())) {
            scopeType = "PATH";
        } else {
            scopeType = "SUBTREE";
        }
        List<Long> visibleOrder = ProfitFlowScope.upwardOrderWithinScope(report.getReportUserId(), u, btgUserMapper);

        return SettlementScopedProfitFlowVO.builder()
                .rootReportId(rootReportId)
                .reportUserId(report.getReportUserId())
                .reportNo(report.getReportNo())
                .reportStatus(report.getStatus())
                .currentHandlerUserId(report.getCurrentHandlerUserId())
                .returnedToApplicant(Boolean.TRUE.equals(report.getReturnedToUser()))
                .visibleUserIdsInOrder(visibleOrder)
                .layers(layers)
                .scopeType(scopeType)
                .build();
    }

    private List<ProfitFlowLayerSummaryVO> buildProfitFlowLayerSummaries(
            ProfitReport report,
            List<SettlementOrder> scopedSettlements,
            Set<Long> visibleUserIds) {
        List<ProfitFlowLayerSummaryVO> out = new ArrayList<>();
        ProfitFlowLayerSummaryVO direct = buildDirectProfitReviewLayer(report, visibleUserIds);
        if (direct != null) {
            out.add(direct);
        }
        if (scopedSettlements == null) {
            return out;
        }
        for (SettlementOrder o : scopedSettlements) {
            if (o.getFromUserId() == null || o.getToUserId() == null) {
                continue;
            }
            BtgUser from = btgUserMapper.selectById(o.getFromUserId());
            BtgUser to = btgUserMapper.selectById(o.getToUserId());
            BigDecimal amt = o.getPayAmount() == null ? null : o.getPayAmount();
            out.add(ProfitFlowLayerSummaryVO.builder()
                    .layerType("SETTLEMENT")
                    .settlementLevelNo(o.getLevelNo())
                    .fromUserId(o.getFromUserId())
                    .toUserId(o.getToUserId())
                    .fromDisplayName(displayName(from))
                    .toDisplayName(displayName(to))
                    .payAmount(amt)
                    .state(mapSettlementToLayerState(o.getStatus()))
                    .build());
        }
        return out;
    }

    private ProfitFlowLayerSummaryVO buildDirectProfitReviewLayer(ProfitReport report, Set<Long> visibleUserIds) {
        Long r = report.getReportUserId();
        Long d = report.getDirectParentUserId();
        if (r == null || d == null || !visibleUserIds.contains(r) || !visibleUserIds.contains(d)) {
            return null;
        }
        ProfitFlowLayerState st = mapProfitReportToDirectLayerState(report.getStatus());
        BtgUser from = btgUserMapper.selectById(r);
        BtgUser to = btgUserMapper.selectById(d);
        return ProfitFlowLayerSummaryVO.builder()
                .layerType("DIRECT_PROFIT_REVIEW")
                .settlementLevelNo(null)
                .fromUserId(r)
                .toUserId(d)
                .fromDisplayName(displayName(from))
                .toDisplayName(displayName(to))
                .payAmount(null)
                .state(st)
                .build();
    }

    private static ProfitFlowLayerState mapProfitReportToDirectLayerState(ProfitReportStatus status) {
        if (status == null) {
            return ProfitFlowLayerState.DIRECT_REVIEW_PASSED;
        }
        return switch (status) {
            case PENDING_DIRECT_REVIEW -> ProfitFlowLayerState.PENDING_DIRECT_REVIEW;
            case RETURNED_TO_APPLICANT -> ProfitFlowLayerState.RETURNED_TO_APPLICANT;
            case REJECTED -> ProfitFlowLayerState.PROFIT_REJECTED;
            case IN_SETTLEMENT_CHAIN, ALL_COMPLETED -> ProfitFlowLayerState.DIRECT_REVIEW_PASSED;
        };
    }

    private static ProfitFlowLayerState mapSettlementToLayerState(SettlementOrderStatus status) {
        if (status == null) {
            return ProfitFlowLayerState.SETTLEMENT_NOT_STARTED;
        }
        return switch (status) {
            case INIT -> ProfitFlowLayerState.SETTLEMENT_NOT_STARTED;
            case PENDING_SUBMIT -> ProfitFlowLayerState.SETTLEMENT_PENDING_SUBMIT;
            case PENDING_REVIEW -> ProfitFlowLayerState.SETTLEMENT_PENDING_REVIEW;
            case APPROVED -> ProfitFlowLayerState.SETTLEMENT_APPROVED;
            case REJECTED -> ProfitFlowLayerState.SETTLEMENT_REJECTED;
        };
    }

    private static String displayName(BtgUser u) {
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
}

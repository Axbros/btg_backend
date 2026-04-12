package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.btg.commission.enums.ProfitReportStatus;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.ProfitAttachmentMapper;
import com.btg.commission.mapper.ProfitReportMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.vo.SettlementOrderDetailVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementOrderService {

    private final SettlementOrderMapper settlementOrderMapper;
    private final ProfitReportMapper profitReportMapper;
    private final BtgUserMapper btgUserMapper;
    private final ProfitAttachmentMapper profitAttachmentMapper;
    private final UserProfileMapper userProfileMapper;
    private final AuditLogService auditLogService;

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

    public Page<SettlementOrder> pageMinePayables(Long userId, long page, long size) {
        Page<SettlementOrder> p = new Page<>(page, size);
        return settlementOrderMapper.selectPage(p, new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getFromUserId, userId)
                .in(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_SUBMIT, SettlementOrderStatus.PENDING_REVIEW)
                .orderByDesc(SettlementOrder::getId));
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

    public Page<SettlementOrder> pagePendingReview(Long userId, long page, long size) {
        Page<SettlementOrder> p = new Page<>(page, size);
        return settlementOrderMapper.selectPage(p, new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getToUserId, userId)
                .eq(SettlementOrder::getStatus, SettlementOrderStatus.PENDING_REVIEW)
                .orderByDesc(SettlementOrder::getId));
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
        if (report == null || report.getStatus() == ProfitReportStatus.REJECTED) {
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

        ProfitReport patch = new ProfitReport();
        patch.setId(o.getRootReportId());
        patch.setStatus(ProfitReportStatus.REJECTED);
        profitReportMapper.updateById(patch);
    }
}

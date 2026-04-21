package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.dto.v1.RepayResubmitRequest;
import com.btg.commission.entity.BtgBusinessFlowLog;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgReplenishmentRepayApply;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.RepayStatusEnum;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.BtgReplenishmentRepayApplyMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.service.BusinessFlowLogService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.service.RepayService;
import com.btg.commission.service.RepayWorkflowService;
import com.btg.commission.service.UserService;
import com.btg.commission.util.FlowLogViewUtil;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.AdminRepayListItemVO;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.RepayMineBriefVO;
import com.btg.commission.vo.RepayPendingReviewListItemVO;
import com.btg.commission.vo.RepayableReplenishmentVO;
import com.btg.commission.vo.ReplenishmentTeamItemVO;
import com.btg.commission.vo.flow.BusinessFlowNodeVO;
import com.btg.commission.vo.flow.RepayApplyFlowDetailVO;
import com.btg.commission.vo.flow.ReplenishmentApplyFlowSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepayServiceImpl implements RepayService {

    private final BtgReplenishmentRepayApplyMapper repayApplyMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final ReplenishmentService replenishmentService;
    private final BusinessFlowLogService businessFlowLogService;
    private final UserService userService;
    private final RepayWorkflowService repayWorkflowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, RepayApplyDTO dto) {
        return repayWorkflowService.submitRepay(userId, dto);
    }

    @Override
    public List<RepayableReplenishmentVO> listRepayableReplenishments(Long currentUserId) {

        List<BtgReplenishmentApply> list = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, currentUserId)
                .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.SUCCESS)
                .isNotNull(BtgReplenishmentApply::getAssignedCapitalUserId)
                .isNotNull(BtgReplenishmentApply::getRemainingAmount)
                .gt(BtgReplenishmentApply::getRemainingAmount, BigDecimal.ZERO)
                .orderByDesc(BtgReplenishmentApply::getId));
        Set<Long> capitalIds = list.stream()
                .map(BtgReplenishmentApply::getAssignedCapitalUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BtgUser> capitals = loadUsersByIds(capitalIds);
        Map<Long, UserProfile> capitalProfiles = loadProfilesByUserIds(capitalIds);
        return list.stream()
                .map(a -> toRepayableVo(a, capitals.get(a.getAssignedCapitalUserId()),
                        capitalProfiles.get(a.getAssignedCapitalUserId())))
                .toList();
    }

    private RepayableReplenishmentVO toRepayableVo(BtgReplenishmentApply a, BtgUser capital, UserProfile capitalProfile) {
        return RepayableReplenishmentVO.builder()
                .id(a.getId())
                .applyNo(a.getApplyNo())
                .approvedAmount(MoneyUtil.money(a.getApprovedAmount()))
                .repaidAmount(MoneyUtil.money(a.getRepaidAmount()))
                .pendingRepayAmount(MoneyUtil.money(a.getPendingRepayAmount()))
                .remainingAmount(MoneyUtil.money(a.getRemainingAmount()))
                .assignedCapitalUserId(a.getAssignedCapitalUserId())
                .assignedCapitalUserName(capital != null ? capital.getNickname() : null)
                .assignedCapitalExchangeUid(capitalProfile != null ? capitalProfile.getExchangeUid() : null)
                .capitalReceiverUid(a.getCapitalReceiverUid())
                .status(a.getStatus() == null ? null : a.getStatus().getValue())
                .auditTime(a.getAuditTime())
                .transferScreenshotUrl(a.getTransferScreenshotUrl())
                .transferRemark(a.getTransferRemark())
                .build();
    }

    @Override
    public Page<RepayMineBriefVO> pageMine(Long userId, long page, long size) {
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getUserId, userId)
                .orderByDesc(BtgReplenishmentRepayApply::getSubmitTime));
        Page<RepayMineBriefVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(RepayServiceImpl::toRepayMineBrief)
                .toList());
        return out;
    }

    private static RepayMineBriefVO toRepayMineBrief(BtgReplenishmentRepayApply e) {
        return RepayMineBriefVO.builder()
                .id(e.getId())
                .repayNo(e.getRepayNo())
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .repayAmount(MoneyUtil.money(e.getRepayAmount()))
                .build();
    }

    @Override
    public Page<AdminRepayListItemVO> pageRepaysForAdmin(long page, long size, Integer status) {
        if (status != null && RepayStatusEnum.fromCode(status) == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "status 须为 1～4 或省略");
        }
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        LambdaQueryWrapper<BtgReplenishmentRepayApply> q = new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .orderByDesc(BtgReplenishmentRepayApply::getId);
        if (status != null) {
            q.eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.fromCode(status));
        }
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, q);
        Map<Long, BtgReplenishmentApply> applyMap = replenishmentByIds(collectReplenishIds(raw.getRecords()));
        Set<Long> applicantIds = raw.getRecords().stream()
                .map(BtgReplenishmentRepayApply::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BtgUser> applicants = loadUsersByIds(applicantIds);
        Page<AdminRepayListItemVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> toAdminRepayListItem(e, applyMap.get(e.getReplenishApplyId()), applicants.get(e.getUserId())))
                .toList());
        return out;
    }

    private static AdminRepayListItemVO toAdminRepayListItem(
            BtgReplenishmentRepayApply e, BtgReplenishmentApply apply, BtgUser applicant) {
        return AdminRepayListItemVO.builder()
                .id(e.getId())
                .repayNo(e.getRepayNo())
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .replenishPendingRepayAmount(apply == null ? null : MoneyUtil.money(apply.getPendingRepayAmount()))
                .applicantNickname(applicantDisplay(applicant))
                .build();
    }

    private static String applicantDisplay(BtgUser u) {
        if (u == null) {
            return null;
        }
        if (u.getNickname() != null && !u.getNickname().isBlank()) {
            return u.getNickname().trim();
        }
        if (u.getMobile() != null && !u.getMobile().isBlank()) {
            return u.getMobile().trim();
        }
        return null;
    }

    @Override
    public Page<RepayPendingReviewListItemVO> pagePendingReviewForCapital(Long capitalUserId, long page, long size) {
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getCapitalUserId, capitalUserId)
                .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.PENDING_CAPITAL_REVIEW)
                .orderByAsc(BtgReplenishmentRepayApply::getSubmitTime));
        if (raw.getRecords().isEmpty()) {
            Page<RepayPendingReviewListItemVO> empty = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        Map<Long, BtgReplenishmentApply> applyMap = replenishmentByIds(collectReplenishIds(raw.getRecords()));
        Page<RepayPendingReviewListItemVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> toRepayPendingReviewListItem(e, applyMap.get(e.getReplenishApplyId())))
                .toList());
        return out;
    }

    private static RepayPendingReviewListItemVO toRepayPendingReviewListItem(
            BtgReplenishmentRepayApply e, BtgReplenishmentApply apply) {
        return RepayPendingReviewListItemVO.builder()
                .id(e.getId())
                .repayNo(e.getRepayNo())
                .pendingRepayAmount(apply == null ? null : MoneyUtil.money(apply.getPendingRepayAmount()))
                .build();
    }

    private static Set<Long> collectReplenishIds(List<BtgReplenishmentRepayApply> records) {
        Set<Long> ids = new HashSet<>();
        for (BtgReplenishmentRepayApply e : records) {
            if (e.getReplenishApplyId() != null) {
                ids.add(e.getReplenishApplyId());
            }
        }
        return ids;
    }

    private Map<Long, BtgReplenishmentApply> replenishmentByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                        .in(BtgReplenishmentApply::getId, ids))
                .stream()
                .collect(Collectors.toMap(BtgReplenishmentApply::getId, Function.identity(), (a, b) -> a));
    }

    @Override
    public Page<ReplenishmentTeamItemVO> pageTeamDescendantRepays(Long viewerUserId, long page, long size) {
        List<Long> descendantIds = userService.listDescendantUserIds(viewerUserId);
        Page<ReplenishmentTeamItemVO> empty = new Page<>(page, size, 0);
        if (descendantIds.isEmpty()) {
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .in(BtgReplenishmentRepayApply::getUserId, descendantIds)
                .orderByDesc(BtgReplenishmentRepayApply::getSubmitTime));
        Set<Long> userIds = raw.getRecords().stream()
                .map(BtgReplenishmentRepayApply::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BtgUser> users = loadUsersByIds(userIds);
        Page<ReplenishmentTeamItemVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> {
                    BtgUser u = users.get(e.getUserId());
                    return ReplenishmentTeamItemVO.builder()
                            .id(e.getId())
                            .status(e.getStatus() == null ? null : e.getStatus().getValue())
                            .nickname(u != null ? u.getNickname() : null)
                            .mobile(u != null ? u.getMobile() : null)
                            .replenishAmount(MoneyUtil.money(e.getRepayAmount()))
                            .build();
                })
                .toList());
        return out;
    }

    private Map<Long, BtgUser> loadUsersByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return btgUserMapper.selectList(new LambdaQueryWrapper<BtgUser>().in(BtgUser::getId, userIds)).stream()
                .collect(Collectors.toMap(BtgUser::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, UserProfile> loadProfilesByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (a, b) -> a));
    }

    @Override
    public RepayApplyVO getRepayDetailForUser(Long viewerUserId, Long repayApplyId) {
        BtgReplenishmentRepayApply e = repayApplyMapper.selectById(repayApplyId);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        assertCanViewRepay(viewerUserId, e);
        BtgUser user = e.getUserId() == null ? null : btgUserMapper.selectById(e.getUserId());
        BtgReplenishmentApply apply = e.getReplenishApplyId() == null ? null : replenishmentApplyMapper.selectById(e.getReplenishApplyId());
        return buildRepayVo(e, user, apply);
    }

    @Override
    public RepayApplyVO getAdminRepayDetail(Long repayApplyId) {
        BtgReplenishmentRepayApply e = repayApplyMapper.selectById(repayApplyId);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        BtgUser user = e.getUserId() == null ? null : btgUserMapper.selectById(e.getUserId());
        BtgReplenishmentApply apply = e.getReplenishApplyId() == null ? null : replenishmentApplyMapper.selectById(e.getReplenishApplyId());
        return buildRepayVo(e, user, apply);
    }

    private void assertCanViewRepay(Long viewerUserId, BtgReplenishmentRepayApply e) {
        if (viewerUserId.equals(e.getUserId())) {
            return;
        }
        if (viewerUserId.equals(e.getCapitalUserId())) {
            return;
        }
        if (e.getUserId() != null && userService.isUpstreamOf(viewerUserId, e.getUserId())) {
            return;
        }
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        if (viewer == null || !Boolean.TRUE.equals(viewer.getIsRoot())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该归仓申请");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRepay(Long capitalUserId, Long repayId, String remark) {
        repayWorkflowService.approveRepay(capitalUserId, repayId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRepay(Long capitalUserId, Long repayId, String remark) {
        repayWorkflowService.rejectRepay(capitalUserId, repayId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long userId, Long repayApplyId, RepayResubmitRequest req) {
        repayWorkflowService.resubmitRepay(userId, repayApplyId, req);
    }

    @Override
    public RepayApplyFlowDetailVO flowDetail(Long viewerUserId, Long repayApplyId) {
        BtgReplenishmentRepayApply e = repayApplyMapper.selectById(repayApplyId);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        assertCanViewRepay(viewerUserId, e);
        List<BtgBusinessFlowLog> logs = businessFlowLogService.listForBusiness(BusinessFlowType.REPLENISHMENT_REPAY_APPLY, repayApplyId);
        List<BusinessFlowNodeVO> nodes = FlowLogViewUtil.toFlowNodes(logs, id -> btgUserMapper.selectById(id));
        boolean everRejected = logs.stream().anyMatch(l ->
                FlowAction.RETURN_TO_APPLICANT.name().equals(l.getAction())
                        || FlowAction.REJECT.name().equals(l.getAction()));
        BtgUser applicant = e.getUserId() == null ? null : btgUserMapper.selectById(e.getUserId());
        BtgReplenishmentApply apply = e.getReplenishApplyId() == null ? null : replenishmentApplyMapper.selectById(e.getReplenishApplyId());
        BtgUser capital = e.getCapitalUserId() == null ? null : btgUserMapper.selectById(e.getCapitalUserId());
        BtgUser handler = e.getCurrentHandlerUserId() == null ? null : btgUserMapper.selectById(e.getCurrentHandlerUserId());
        ReplenishmentApplyFlowSummaryVO linked = apply == null ? null : ReplenishmentApplyFlowSummaryVO.builder()
                .id(apply.getId())
                .applyNo(apply.getApplyNo())
                .status(apply.getStatus())
                .replenishAmount(MoneyUtil.money(apply.getReplenishAmount()))
                .remainingAmount(MoneyUtil.money(apply.getRemainingAmount()))
                .build();
        return RepayApplyFlowDetailVO.builder()
                .repay(buildRepayVo(e, applicant, apply))
                .linkedReplenishment(linked)
                .applicantUserId(e.getUserId())
                .applicantNickname(applicant != null ? applicant.getNickname() : null)
                .currentHandlerUserId(e.getCurrentHandlerUserId())
                .currentHandlerUserName(handler != null ? handler.getNickname() : null)
                .currentStatus(e.getStatus())
                .returnedToApplicant(Boolean.TRUE.equals(e.getReturnedToUser()))
                .everRejected(everRejected)
                .submitVersion(e.getSubmitVersion() == null ? 1 : e.getSubmitVersion())
                .lastRejectReason(e.getLastRejectReason())
                .capitalUserId(e.getCapitalUserId())
                .capitalUserName(capital != null ? capital.getNickname() : null)
                .capitalReceiverUid(e.getCapitalReceiverUid())
                .nodes(nodes)
                .build();
    }

    private RepayApplyVO buildRepayVo(BtgReplenishmentRepayApply e, BtgUser user, BtgReplenishmentApply apply) {
        BtgUser capital = e.getCapitalUserId() == null ? null : btgUserMapper.selectById(e.getCapitalUserId());
        BtgUser handler = e.getCurrentHandlerUserId() == null ? null : btgUserMapper.selectById(e.getCurrentHandlerUserId());
        RepayApplyVO.RepayApplyVOBuilder b = RepayApplyVO.builder()
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
                .capitalUserId(e.getCapitalUserId())
                .capitalUserName(capital != null ? capital.getNickname() : null)
                .capitalReceiverUid(e.getCapitalReceiverUid())
                .currentHandlerUserId(e.getCurrentHandlerUserId())
                .currentHandlerUserName(handler != null ? handler.getNickname() : null)
                .submitVersion(e.getSubmitVersion() == null ? 1 : e.getSubmitVersion())
                .lastRejectReason(e.getLastRejectReason());
        if (user != null) {
            b.nickname(user.getNickname());
            b.mobile(user.getMobile());
        }
        if (apply != null) {
            b.replenishmentApply(replenishmentService.toApplyVo(apply));
        }
        return b.build();
    }
}

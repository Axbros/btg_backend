package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.ReplenishmentAssignCapitalRequest;
import com.btg.commission.dto.v1.ReplenishmentCapitalSubmitRequest;
import com.btg.commission.dto.v1.ReplenishmentResubmitRequest;
import com.btg.commission.entity.BtgBusinessFlowLog;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgReplenishmentRepayApply;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;
import com.btg.commission.enums.RepayStatusEnum;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.BtgReplenishmentRepayApplyMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.service.AuditLogService;
import com.btg.commission.service.BusinessFlowLogService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.service.ReplenishmentWorkflowService;
import com.btg.commission.service.UserQualificationGateService;
import com.btg.commission.service.UserService;
import com.btg.commission.util.FlowLogViewUtil;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.ReplenishmentApplyBriefVO;
import com.btg.commission.vo.ReplenishmentApplyDetailVO;
import com.btg.commission.vo.ReplenishmentApplyVO;
import com.btg.commission.vo.ReplenishmentPendingBriefVO;
import com.btg.commission.vo.ReplenishmentTeamItemVO;
import com.btg.commission.vo.flow.BusinessFlowNodeVO;
import com.btg.commission.vo.flow.ReplenishmentApplyFlowDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ReplenishmentServiceImpl implements ReplenishmentService {

    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final BtgReplenishmentRepayApplyMapper repayApplyMapper;
    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final AuditLogService auditLogService;
    private final BusinessFlowLogService businessFlowLogService;
    private final UserService userService;
    private final UserQualificationGateService userQualificationGateService;
    private final ReplenishmentWorkflowService replenishmentWorkflowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, ReplenishmentApplyDTO dto) {
        userQualificationGateService.requireApprovedForFormalBusiness(userId);
        if (replenishmentApplyMapper.existsBlockingNewReplenishmentByUserId(userId)) {
            throw new BizException(ResultCode.CONFLICT, "存在进行中的补仓申请，请勿重复提交");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "请先完善用户资料");
        }
        BigDecimal principal = MoneyUtil.money(profile.getPrincipalAmount());
        BigDecimal balance = MoneyUtil.money(dto.getBalanceAmount());
        BigDecimal replenish = MoneyUtil.money(principal.subtract(balance));
        if (replenish.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "补仓额度须大于 0（底仓本金应大于当前余额）");
        }
        if (!StringUtils.hasText(dto.getBalanceScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传余额截图");
        }
        Long adminQueueHolder = requireRootUserId();
        BtgReplenishmentApply row = new BtgReplenishmentApply();
        row.setApplyNo(nextApplyNo());
        row.setUserId(userId);
        row.setPrincipalAmount(principal);
        row.setBalanceAmount(balance);
        row.setReplenishAmount(replenish);
        row.setBalanceScreenshotUrl(dto.getBalanceScreenshotUrl().trim());
        row.setStatus(ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW);
        row.setApprovedAmount(MoneyUtil.money(null));
        row.setRepaidAmount(MoneyUtil.money(null));
        row.setPendingRepayAmount(MoneyUtil.money(null));
        row.setRemainingAmount(MoneyUtil.money(null));
        row.setSubmitTime(LocalDateTime.now());
        row.setSubmitVersion(1);
        row.setCurrentHandlerUserId(adminQueueHolder);
        row.setReturnedToUser(false);
        row.setFlowStatus(ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW.name());
        replenishmentApplyMapper.insert(row);
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.SUBMIT, userId, null);
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_APPLY,
                row.getId(),
                null,
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.SUBMIT,
                ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW.name(),
                1,
                null,
                userId);
        return row.getId();
    }

    @Override
    public Page<ReplenishmentApplyBriefVO> pageMine(Long userId, long page, long size) {
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .orderByDesc(BtgReplenishmentApply::getSubmitTime));
        List<BtgReplenishmentApply> records = raw.getRecords();
        Page<ReplenishmentApplyBriefVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(records.stream()
                .map(e -> new ReplenishmentApplyBriefVO(
                        e.getId(),
                        e.getApplyNo(),
                        e.getStatus() == null ? null : e.getStatus().getValue()))
                .toList());
        return out;
    }

    @Override
    public Page<ReplenishmentApplyBriefVO> pageAssignedToMe(Long capitalUserId, long page, long size) {
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getAssignedCapitalUserId, capitalUserId)
                .in(BtgReplenishmentApply::getStatus,
                        ReplenishmentStatusEnum.PENDING_CAPITAL_SUBMIT,
                        ReplenishmentStatusEnum.RETURNED_TO_CAPITAL)
                .orderByDesc(BtgReplenishmentApply::getSubmitTime));
        Page<ReplenishmentApplyBriefVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> new ReplenishmentApplyBriefVO(
                        e.getId(),
                        e.getApplyNo(),
                        e.getStatus() == null ? null : e.getStatus().getValue()))
                .toList());
        return out;
    }

    @Override
    public ReplenishmentApplyDetailVO getReplenishmentDetailForUser(Long viewerUserId, Long applyId) {
        BtgReplenishmentApply apply = replenishmentApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        assertCanViewReplenishment(viewerUserId, apply);
        List<BtgReplenishmentRepayApply> repays = repayApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getReplenishApplyId, applyId)
                .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.APPROVED)
                .orderByDesc(BtgReplenishmentRepayApply::getSubmitTime));
        Set<Long> repayRelatedUserIds = new HashSet<>();
        for (BtgReplenishmentRepayApply r : repays) {
            if (r.getUserId() != null) {
                repayRelatedUserIds.add(r.getUserId());
            }
            if (r.getCapitalUserId() != null) {
                repayRelatedUserIds.add(r.getCapitalUserId());
            }
            if (r.getCurrentHandlerUserId() != null) {
                repayRelatedUserIds.add(r.getCurrentHandlerUserId());
            }
        }
        Map<Long, BtgUser> userMap = loadUsersByIds(repayRelatedUserIds);
        List<RepayApplyVO> repayVos = repays.stream()
                .map(r -> repayToVoShallow(r, userMap))
                .toList();
        BtgUser applicant = apply.getUserId() == null ? null : btgUserMapper.selectById(apply.getUserId());
        return ReplenishmentApplyDetailVO.builder()
                .replenishment(toVo(apply, profileOf(apply.getUserId()), applicant))
                .approvedRepays(repayVos)
                .build();
    }

    @Override
    public Page<ReplenishmentTeamItemVO> pageTeamDescendantApplies(Long viewerUserId, long page, long size) {
        List<Long> descendantIds = userService.listDescendantUserIds(viewerUserId);
        Page<ReplenishmentTeamItemVO> empty = new Page<>(page, size, 0);
        if (descendantIds.isEmpty()) {
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .in(BtgReplenishmentApply::getUserId, descendantIds)
                .orderByDesc(BtgReplenishmentApply::getSubmitTime));
        Set<Long> userIds = raw.getRecords().stream().map(BtgReplenishmentApply::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
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
                            .replenishAmount(MoneyUtil.money(e.getReplenishAmount()))
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

    private static RepayApplyVO repayToVoShallow(BtgReplenishmentRepayApply e, Map<Long, BtgUser> users) {
        BtgUser user = e.getUserId() == null ? null : users.get(e.getUserId());
        BtgUser capital = e.getCapitalUserId() == null ? null : users.get(e.getCapitalUserId());
        BtgUser handler = e.getCurrentHandlerUserId() == null ? null : users.get(e.getCurrentHandlerUserId());
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
                .lastRejectReason(e.getLastRejectReason())
                .replenishmentApply(null);
        if (user != null) {
            b.nickname(user.getNickname());
            b.mobile(user.getMobile());
        }
        return b.build();
    }

    @Override
    public ReplenishmentApplyVO current(Long userId) {
        BtgReplenishmentApply one = replenishmentApplyMapper.selectOne(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, userId)
                .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.SUCCESS)
                .isNotNull(BtgReplenishmentApply::getRemainingAmount)
                .gt(BtgReplenishmentApply::getRemainingAmount, BigDecimal.ZERO)
                .orderByDesc(BtgReplenishmentApply::getId)
                .last("LIMIT 1"));
        return one == null ? null : toVo(one, profileOf(one.getUserId()), btgUserMapper.selectById(userId));
    }

    @Override
    public Page<ReplenishmentPendingBriefVO> pagePendingForAdmin(long page, long size) {
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW)
                .orderByAsc(BtgReplenishmentApply::getSubmitTime));
        Set<Long> userIds = raw.getRecords().stream().map(BtgReplenishmentApply::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BtgUser> users = loadUsersByIds(userIds);
        Page<ReplenishmentPendingBriefVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> {
                    BtgUser u = users.get(e.getUserId());
                    return ReplenishmentPendingBriefVO.builder()
                            .id(e.getId())
                            .nickname(u != null ? u.getNickname() : null)
                            .mobile(u != null ? u.getMobile() : null)
                            .replenishAmount(MoneyUtil.money(e.getReplenishAmount()))
                            .build();
                })
                .toList());
        return out;
    }

    @Override
    public Page<ReplenishmentApplyVO> pageAllForAdmin(long page, long size) {
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .orderByDesc(BtgReplenishmentApply::getId));
        Set<Long> userIds = raw.getRecords().stream().map(BtgReplenishmentApply::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BtgUser> users = loadUsersByIds(userIds);
        Page<ReplenishmentApplyVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> toVo(e, profileOf(e.getUserId()), users.get(e.getUserId())))
                .toList());
        return out;
    }

    @Override
    public ReplenishmentApplyVO getReplenishmentDetailForAdmin(Long applyId) {
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        return toVo(row, profileOf(row.getUserId()), row.getUserId() == null ? null : btgUserMapper.selectById(row.getUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveByAdmin(Long applyId, Long adminUserId, String remark) {
        replenishmentWorkflowService.approveByAdmin(applyId, adminUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByAdmin(Long applyId, Long adminUserId, String remark) {
        replenishmentWorkflowService.rejectByAdmin(applyId, adminUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignCapital(Long applyId, Long adminUserId, ReplenishmentAssignCapitalRequest req) {
        if (req == null || req.getCapitalUserId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求体无效");
        }
        replenishmentWorkflowService.assignCapital(applyId, adminUserId, req.getCapitalUserId(), req.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void capitalSubmit(Long capitalUserId, Long applyId, ReplenishmentCapitalSubmitRequest dto) {
        userQualificationGateService.requireApprovedForFormalBusiness(capitalUserId);
        replenishmentWorkflowService.capitalSubmit(applyId, capitalUserId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmArrival(Long applicantUserId, Long applyId, String remark) {
        userQualificationGateService.requireApprovedForFormalBusiness(applicantUserId);
        replenishmentWorkflowService.confirmArrival(applyId, applicantUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectArrival(Long applicantUserId, Long applyId, String remark) {
        userQualificationGateService.requireApprovedForFormalBusiness(applicantUserId);
        replenishmentWorkflowService.rejectArrival(applyId, applicantUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long userId, Long applyId, ReplenishmentResubmitRequest req) {
        userQualificationGateService.requireApprovedForFormalBusiness(userId);
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (!userId.equals(row.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申请人可重新提交");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.REJECTED) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可重新提交补仓申请");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "请先完善用户资料");
        }
        BigDecimal principal = MoneyUtil.money(profile.getPrincipalAmount());
        BigDecimal balance = MoneyUtil.money(req.getBalanceAmount());
        BigDecimal replenish = MoneyUtil.money(principal.subtract(balance));
        if (replenish.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "补仓额度须大于 0（底仓本金应大于当前余额）");
        }
        int nextVer = (row.getSubmitVersion() == null ? 1 : row.getSubmitVersion()) + 1;
        Long adminQueueHolder = requireRootUserId();
        replenishmentApplyMapper.update(
                null,
                new LambdaUpdateWrapper<BtgReplenishmentApply>()
                        .eq(BtgReplenishmentApply::getId, applyId)
                        .set(BtgReplenishmentApply::getPrincipalAmount, principal)
                        .set(BtgReplenishmentApply::getBalanceAmount, balance)
                        .set(BtgReplenishmentApply::getReplenishAmount, replenish)
                        .set(BtgReplenishmentApply::getBalanceScreenshotUrl, req.getBalanceScreenshotUrl().trim())
                        .set(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW)
                        .set(BtgReplenishmentApply::getSubmitTime, LocalDateTime.now())
                        .set(BtgReplenishmentApply::getSubmitVersion, nextVer)
                        .set(BtgReplenishmentApply::getCurrentHandlerUserId, adminQueueHolder)
                        .set(BtgReplenishmentApply::getReturnedToUser, false)
                        .set(BtgReplenishmentApply::getFlowStatus, ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW.name())
                        .set(BtgReplenishmentApply::getLastRejectReason, null)
                        .set(BtgReplenishmentApply::getLastRejectTime, null)
                        .set(BtgReplenishmentApply::getLastRejectBy, null)
                        .set(BtgReplenishmentApply::getAuditRemark, null)
                        .set(BtgReplenishmentApply::getAuditBy, null)
                        .set(BtgReplenishmentApply::getAuditTime, null)
                        .set(BtgReplenishmentApply::getAssignedCapitalUserId, null)
                        .set(BtgReplenishmentApply::getAssignedBy, null)
                        .set(BtgReplenishmentApply::getAssignedTime, null)
                        .set(BtgReplenishmentApply::getAssignRemark, null)
                        .set(BtgReplenishmentApply::getTransferScreenshotUrl, null)
                        .set(BtgReplenishmentApply::getTransferRemark, null)
                        .set(BtgReplenishmentApply::getCapitalSubmitTime, null)
                        .set(BtgReplenishmentApply::getCapitalSubmitRemark, null)
                        .set(BtgReplenishmentApply::getCapitalReceiverUid, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmStatus, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmTime, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmBy, null)
                        .set(BtgReplenishmentApply::getArrivalConfirmRemark, null));
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.RESUBMIT, userId, "resubmit");
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_APPLY,
                applyId,
                null,
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.RESUBMIT,
                ReplenishmentStatusEnum.PENDING_ADMIN_REVIEW.name(),
                nextVer,
                null,
                userId);
    }

    @Override
    public ReplenishmentApplyFlowDetailVO flowDetail(Long viewerUserId, Long applyId) {
        BtgReplenishmentApply apply = replenishmentApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        assertCanViewReplenishment(viewerUserId, apply);
        List<BtgBusinessFlowLog> logs = businessFlowLogService.listForBusiness(BusinessFlowType.REPLENISHMENT_APPLY, applyId);
        List<BusinessFlowNodeVO> nodes = FlowLogViewUtil.toFlowNodes(logs, id -> btgUserMapper.selectById(id));
        boolean everRejected = logs.stream().anyMatch(l ->
                FlowAction.RETURN_TO_APPLICANT.name().equals(l.getAction())
                        || FlowAction.REJECT.name().equals(l.getAction()));
        BtgUser applicant = apply.getUserId() == null ? null : btgUserMapper.selectById(apply.getUserId());
        return ReplenishmentApplyFlowDetailVO.builder()
                .apply(toVo(apply, profileOf(apply.getUserId()), applicant))
                .applicantUserId(apply.getUserId())
                .applicantNickname(applicant != null ? applicant.getNickname() : null)
                .currentHandlerUserId(apply.getCurrentHandlerUserId())
                .currentStatus(apply.getStatus())
                .returnedToApplicant(Boolean.TRUE.equals(apply.getReturnedToUser()))
                .everRejected(everRejected)
                .submitVersion(apply.getSubmitVersion() == null ? 1 : apply.getSubmitVersion())
                .lastRejectReason(apply.getLastRejectReason())
                .nodes(nodes)
                .build();
    }

    private void assertCanViewReplenishment(Long viewerUserId, BtgReplenishmentApply apply) {
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        if (viewer != null && Boolean.TRUE.equals(viewer.getIsRoot())) {
            return;
        }
        if (viewerUserId.equals(apply.getUserId())) {
            return;
        }
        if (apply.getAssignedCapitalUserId() != null && viewerUserId.equals(apply.getAssignedCapitalUserId())) {
            return;
        }
        if (userService.isUpstreamOf(viewerUserId, apply.getUserId())) {
            return;
        }
        throw new BizException(ResultCode.FORBIDDEN, "无权查看该补仓申请");
    }

    private Long requireRootUserId() {
        BtgUser root = btgUserMapper.selectOne(new LambdaQueryWrapper<BtgUser>()
                .eq(BtgUser::getIsRoot, true)
                .last("LIMIT 1"));
        if (root == null) {
            throw new BizException(ResultCode.CONFLICT, "系统未配置根用户，无法处理补仓审核");
        }
        return root.getId();
    }

    private static String nextApplyNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "RF" + ts + rnd;
    }

    @Override
    public ReplenishmentApplyVO toApplyVo(BtgReplenishmentApply e) {
        if (e == null) {
            return null;
        }
        return toVo(e, profileOf(e.getUserId()), e.getUserId() == null ? null : btgUserMapper.selectById(e.getUserId()));
    }

    private UserProfile profileOf(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
    }

    private static String trimOrEmptyWallet(String s) {
        return StringUtils.hasText(s) ? s.trim() : "";
    }

    private ReplenishmentApplyVO toVo(BtgReplenishmentApply e, UserProfile profile, BtgUser user) {
        BtgUser assigned = e.getAssignedCapitalUserId() == null ? null : btgUserMapper.selectById(e.getAssignedCapitalUserId());
        return ReplenishmentApplyVO.builder()
                .id(e.getId())
                .applyNo(e.getApplyNo())
                .userId(e.getUserId())
                .nickname(user != null ? user.getNickname() : null)
                .mobile(user != null ? user.getMobile() : null)
                .principalAmount(e.getPrincipalAmount())
                .balanceAmount(e.getBalanceAmount())
                .replenishAmount(e.getReplenishAmount())
                .balanceScreenshotUrl(e.getBalanceScreenshotUrl())
                .transferScreenshotUrl(e.getTransferScreenshotUrl())
                .transferRemark(e.getTransferRemark())
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .walletName(trimOrEmptyWallet(profile != null ? profile.getWalletName() : null))
                .walletAddress(trimOrEmptyWallet(profile != null ? profile.getWalletAddress() : null))
                .acceptedAt(e.getAcceptedAt())
                .acceptedBy(e.getAcceptedBy())
                .approvedAmount(e.getApprovedAmount())
                .repaidAmount(e.getRepaidAmount())
                .pendingRepayAmount(e.getPendingRepayAmount())
                .remainingAmount(e.getRemainingAmount())
                .submitTime(e.getSubmitTime())
                .auditTime(e.getAuditTime())
                .auditBy(e.getAuditBy())
                .auditRemark(e.getAuditRemark())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .currentHandlerUserId(e.getCurrentHandlerUserId())
                .assignedCapitalUserId(e.getAssignedCapitalUserId())
                .assignedCapitalNickname(assigned != null ? assigned.getNickname() : null)
                .assignedBy(e.getAssignedBy())
                .assignedTime(e.getAssignedTime())
                .assignRemark(e.getAssignRemark())
                .capitalSubmitTime(e.getCapitalSubmitTime())
                .capitalSubmitRemark(e.getCapitalSubmitRemark())
                .capitalReceiverUid(e.getCapitalReceiverUid())
                .arrivalConfirmStatus(e.getArrivalConfirmStatus() == null ? null : e.getArrivalConfirmStatus().getValue())
                .arrivalConfirmTime(e.getArrivalConfirmTime())
                .arrivalConfirmBy(e.getArrivalConfirmBy())
                .arrivalConfirmRemark(e.getArrivalConfirmRemark())
                .build();
    }
}

package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.ReplenishmentApplyDTO;
import com.btg.commission.dto.v1.ReplenishmentApproveDTO;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, ReplenishmentApplyDTO dto) {
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
        BtgReplenishmentApply row = new BtgReplenishmentApply();
        row.setApplyNo(nextApplyNo());
        row.setUserId(userId);
        row.setPrincipalAmount(principal);
        row.setBalanceAmount(balance);
        row.setReplenishAmount(replenish);
        row.setBalanceScreenshotUrl(dto.getBalanceScreenshotUrl().trim());
        row.setStatus(ReplenishmentStatusEnum.PENDING_AUDIT);
        row.setApprovedAmount(MoneyUtil.money(null));
        row.setRepaidAmount(MoneyUtil.money(null));
        row.setPendingRepayAmount(MoneyUtil.money(null));
        row.setRemainingAmount(MoneyUtil.money(null));
        row.setSubmitTime(LocalDateTime.now());
        row.setSubmitVersion(1);
        row.setCurrentHandlerUserId(requireRootUserId());
        row.setReturnedToUser(false);
        row.setFlowStatus("PENDING_AUDIT");
        replenishmentApplyMapper.insert(row);
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.SUBMIT, userId, null);
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_APPLY,
                row.getId(),
                null,
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.SUBMIT,
                ReplenishmentStatusEnum.PENDING_AUDIT.name(),
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
    public ReplenishmentApplyDetailVO getReplenishmentDetailForUser(Long viewerUserId, Long applyId) {
        BtgReplenishmentApply apply = replenishmentApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (!viewerUserId.equals(apply.getUserId()) && !userService.isUpstreamOf(viewerUserId, apply.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该补仓申请");
        }
        List<BtgReplenishmentRepayApply> repays = repayApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getReplenishApplyId, applyId)
                .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.APPROVED)
                .orderByDesc(BtgReplenishmentRepayApply::getSubmitTime));
        Set<Long> repayUserIds = repays.stream()
                .map(BtgReplenishmentRepayApply::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BtgUser> userMap = loadUsersByIds(repayUserIds);
        List<RepayApplyVO> repayVos = repays.stream()
                .map(r -> repayToVoShallow(r, userMap.get(r.getUserId())))
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

    private static RepayApplyVO repayToVoShallow(BtgReplenishmentRepayApply e, BtgUser user) {
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
                .in(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.APPROVED, ReplenishmentStatusEnum.PARTIALLY_REPAID)
                .orderByDesc(BtgReplenishmentApply::getId)
                .last("LIMIT 1"));
        return one == null ? null : toVo(one, profileOf(one.getUserId()), btgUserMapper.selectById(userId));
    }

    @Override
    public Page<ReplenishmentPendingBriefVO> pagePendingForAdmin(long page, long size) {
        Page<BtgReplenishmentApply> p = new Page<>(page, size);
        Page<BtgReplenishmentApply> raw = replenishmentApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentApply>()
                .in(BtgReplenishmentApply::getStatus,
                        ReplenishmentStatusEnum.PENDING_AUDIT,
                        ReplenishmentStatusEnum.PENDING_SUPPLEMENT,
                        ReplenishmentStatusEnum.PENDING_TRANSFER)
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
    public ReplenishmentApplyVO getReplenishmentDetailForAdmin(Long applyId) {
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        return toVo(row, profileOf(row.getUserId()), row.getUserId() == null ? null : btgUserMapper.selectById(row.getUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptForAdmin(Long applyId, Long adminUserId) {
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_AUDIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可受理");
        }
        row.setStatus(ReplenishmentStatusEnum.PENDING_SUPPLEMENT);
        row.setAcceptedAt(LocalDateTime.now());
        row.setAcceptedBy(adminUserId);
        replenishmentApplyMapper.updateById(row);
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.SUBMIT, adminUserId, "资方已受理补仓申请");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitCapitalVoucherForAdmin(Long adminUserId, Long applyId, ReplenishmentApproveDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求体不能为空");
        }
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        ReplenishmentStatusEnum st = row.getStatus();
        if (st != ReplenishmentStatusEnum.PENDING_SUPPLEMENT && st != ReplenishmentStatusEnum.PENDING_TRANSFER) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可上传或修改资方凭证（须在终审前且为待上传凭证或待终审）");
        }
        boolean hasNewUrl = StringUtils.hasText(dto.getTransferScreenshotUrl());
        String trimmedUrl = hasNewUrl ? dto.getTransferScreenshotUrl().trim() : null;
        row.setTransferRemark(trimOrNull(dto.getTransferRemark()));

        if (st == ReplenishmentStatusEnum.PENDING_SUPPLEMENT) {
            if (!hasNewUrl) {
                throw new BizException(ResultCode.BAD_REQUEST, "请上传资方转账凭证");
            }
            row.setTransferScreenshotUrl(trimmedUrl);
            row.setStatus(ReplenishmentStatusEnum.PENDING_TRANSFER);
            replenishmentApplyMapper.updateById(row);
            auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.SUBMIT, adminUserId, "资方已上传转账凭证与备注");
        } else {
            if (hasNewUrl) {
                row.setTransferScreenshotUrl(trimmedUrl);
            }
            if (!StringUtils.hasText(row.getTransferScreenshotUrl())) {
                throw new BizException(ResultCode.BAD_REQUEST, "请上传资方转账凭证");
            }
            replenishmentApplyMapper.updateById(row);
            auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.SUBMIT, adminUserId, "资方已更新转账凭证或备注");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveForAdmin(Long applyId, Long adminUserId) {
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_TRANSFER) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可终审通过（须先受理并上传资方凭证）");
        }
        if (!StringUtils.hasText(row.getTransferScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请先完成资方凭证上传");
        }
        BigDecimal replenish = MoneyUtil.money(row.getReplenishAmount());
        row.setStatus(ReplenishmentStatusEnum.APPROVED);
        row.setApprovedAmount(replenish);
        row.setRemainingAmount(replenish);
        row.setRepaidAmount(MoneyUtil.money(null));
        row.setPendingRepayAmount(MoneyUtil.money(null));
        row.setAuditBy(adminUserId);
        row.setAuditTime(LocalDateTime.now());
        row.setAuditRemark(null);
        replenishmentApplyMapper.updateById(row);
        auditLogService.log(
                AuditBusinessType.REPLENISHMENT_APPLY,
                row.getId(),
                AuditAction.APPROVE,
                adminUserId,
                "资方终审确认通过补仓申请");
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_APPLY,
                row.getId(),
                null,
                row.getUserId(),
                FlowNodeRole.CAPITAL,
                FlowAction.APPROVE,
                ReplenishmentStatusEnum.APPROVED.name(),
                row.getSubmitVersion() == null ? 1 : row.getSubmitVersion(),
                null,
                adminUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectForAdmin(Long applyId, Long adminUserId, String remark) {
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.PENDING_AUDIT
                && row.getStatus() != ReplenishmentStatusEnum.PENDING_SUPPLEMENT
                && row.getStatus() != ReplenishmentStatusEnum.PENDING_TRANSFER) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
        }
        LocalDateTime now = LocalDateTime.now();
        BtgReplenishmentApply patch = new BtgReplenishmentApply();
        patch.setId(applyId);
        patch.setStatus(ReplenishmentStatusEnum.RETURNED_TO_APPLICANT);
        patch.setAuditBy(adminUserId);
        patch.setAuditTime(now);
        patch.setAuditRemark(trimOrNull(remark));
        patch.setCurrentHandlerUserId(row.getUserId());
        patch.setReturnedToUser(true);
        patch.setFlowStatus("RETURNED_TO_APPLICANT");
        patch.setLastRejectReason(trimOrNull(remark));
        patch.setLastRejectTime(now);
        patch.setLastRejectBy(adminUserId);
        patch.setTransferScreenshotUrl(null);
        patch.setTransferRemark(null);
        patch.setAcceptedAt(null);
        patch.setAcceptedBy(null);
        replenishmentApplyMapper.updateById(patch);
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, row.getId(), AuditAction.REJECT, adminUserId, remark);
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_APPLY,
                row.getId(),
                null,
                row.getUserId(),
                FlowNodeRole.CAPITAL,
                FlowAction.RETURN_TO_APPLICANT,
                ReplenishmentStatusEnum.RETURNED_TO_APPLICANT.name(),
                row.getSubmitVersion() == null ? 1 : row.getSubmitVersion(),
                remark,
                adminUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long userId, Long applyId, ReplenishmentResubmitRequest req) {
        BtgReplenishmentApply row = replenishmentApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在");
        }
        if (!userId.equals(row.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅申请人可重新提交");
        }
        if (row.getStatus() != ReplenishmentStatusEnum.RETURNED_TO_APPLICANT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可重新提交");
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
        Long rootId = requireRootUserId();
        BtgReplenishmentApply patch = new BtgReplenishmentApply();
        patch.setId(applyId);
        patch.setPrincipalAmount(principal);
        patch.setBalanceAmount(balance);
        patch.setReplenishAmount(replenish);
        patch.setBalanceScreenshotUrl(req.getBalanceScreenshotUrl().trim());
        patch.setStatus(ReplenishmentStatusEnum.PENDING_AUDIT);
        patch.setSubmitTime(LocalDateTime.now());
        patch.setSubmitVersion(nextVer);
        patch.setCurrentHandlerUserId(rootId);
        patch.setReturnedToUser(false);
        patch.setFlowStatus("PENDING_AUDIT");
        patch.setLastRejectReason(null);
        patch.setLastRejectTime(null);
        patch.setLastRejectBy(null);
        patch.setAuditRemark(null);
        patch.setAuditBy(null);
        patch.setAuditTime(null);
        replenishmentApplyMapper.updateById(patch);
        auditLogService.log(AuditBusinessType.REPLENISHMENT_APPLY, applyId, AuditAction.SUBMIT, userId, "resubmit");
        businessFlowLogService.append(
                BusinessFlowType.REPLENISHMENT_APPLY,
                applyId,
                null,
                userId,
                FlowNodeRole.APPLICANT,
                FlowAction.RESUBMIT,
                ReplenishmentStatusEnum.PENDING_AUDIT.name(),
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
        boolean root = viewer != null && Boolean.TRUE.equals(viewer.getIsRoot());
        if (root) {
            return;
        }
        if (viewerUserId.equals(apply.getUserId())) {
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
            throw new BizException(ResultCode.CONFLICT, "系统未配置根用户，无法处理补仓/归仓审核");
        }
        return root.getId();
    }

    private static String trimOrNull(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        return remark.trim();
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

    private Map<Long, UserProfile> profilesByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (a, b) -> a));
    }

    private static String trimOrEmptyWallet(String s) {
        return StringUtils.hasText(s) ? s.trim() : "";
    }

    private ReplenishmentApplyVO toVo(BtgReplenishmentApply e, UserProfile profile) {
        return toVo(e, profile, null);
    }

    private ReplenishmentApplyVO toVo(BtgReplenishmentApply e, UserProfile profile, BtgUser user) {
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
                .build();
    }
}

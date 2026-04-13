package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.RepayApplyDTO;
import com.btg.commission.entity.BtgReplenishmentApply;
import com.btg.commission.entity.BtgReplenishmentRepayApply;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.enums.RepayStatusEnum;
import com.btg.commission.enums.ReplenishmentStatusEnum;
import com.btg.commission.mapper.BtgReplenishmentApplyMapper;
import com.btg.commission.mapper.BtgReplenishmentRepayApplyMapper;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.service.AuditLogService;
import com.btg.commission.service.ReplenishmentService;
import com.btg.commission.service.RepayService;
import com.btg.commission.service.UserService;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.RepayApplyVO;
import com.btg.commission.vo.RepayPendingBriefVO;
import com.btg.commission.vo.RepayableReplenishmentVO;
import com.btg.commission.vo.ReplenishmentTeamItemVO;
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
public class RepayServiceImpl implements RepayService {

    private final BtgReplenishmentRepayApplyMapper repayApplyMapper;
    private final BtgReplenishmentApplyMapper replenishmentApplyMapper;
    private final BtgUserMapper btgUserMapper;
    private final ReplenishmentService replenishmentService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, RepayApplyDTO dto) {
        BtgReplenishmentApply parent = loadReplenishmentForSubmit(dto.getReplenishApplyId(), userId);

        if (!StringUtils.hasText(dto.getRepayScreenshotUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请上传归仓转账截图");
        }
        BigDecimal repay = MoneyUtil.money(dto.getRepayAmount());
        if (repay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额须大于 0");
        }
        BigDecimal remaining = MoneyUtil.money(parent.getRemainingAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        if (repay.compareTo(remaining) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额不能超过剩余应还金额");
        }
        BigDecimal ceiling = MoneyUtil.money(remaining.subtract(pending));
        if (repay.compareTo(ceiling) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "归还金额超过可归仓上限（含待审核归仓）");
        }

        BtgReplenishmentRepayApply row = new BtgReplenishmentRepayApply();
        row.setRepayNo(nextRepayNo());
        row.setReplenishApplyId(dto.getReplenishApplyId());
        row.setUserId(userId);
        row.setRepayAmount(repay);
        row.setRepayScreenshotUrl(dto.getRepayScreenshotUrl().trim());
        row.setStatus(RepayStatusEnum.PENDING_AUDIT);
        row.setSubmitTime(LocalDateTime.now());
        repayApplyMapper.insert(row);

        parent.setPendingRepayAmount(MoneyUtil.money(pending.add(repay)));
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, row.getId(), AuditAction.SUBMIT, userId, null);
        return row.getId();
    }

    /**
     * 按补仓主键加载；{@link com.baomidou.mybatisplus.annotation.TableLogic} 已过滤 deleted_at。
     */
    private BtgReplenishmentApply loadReplenishmentForSubmit(Long replenishApplyId, Long userId) {
        if (replenishApplyId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "补仓申请ID不能为空");
        }
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectById(replenishApplyId);
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "补仓申请不存在或已删除");
        }
        if (!userId.equals(parent.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权对该补仓单提交归仓");
        }
        if (parent.getStatus() != ReplenishmentStatusEnum.APPROVED
                && parent.getStatus() != ReplenishmentStatusEnum.PARTIALLY_REPAID) {
            throw new BizException(ResultCode.CONFLICT, "该补仓单未处于可归仓状态");
        }
        BigDecimal remaining = MoneyUtil.money(parent.getRemainingAmount());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.CONFLICT, "该补仓单已无可归仓金额");
        }
        return parent;
    }

    @Override
    public List<RepayableReplenishmentVO> listRepayableReplenishments(Long currentUserId) {
        List<BtgReplenishmentApply> list = replenishmentApplyMapper.selectList(new LambdaQueryWrapper<BtgReplenishmentApply>()
                .eq(BtgReplenishmentApply::getUserId, currentUserId)
                .in(BtgReplenishmentApply::getStatus, ReplenishmentStatusEnum.APPROVED, ReplenishmentStatusEnum.PARTIALLY_REPAID)
                .isNotNull(BtgReplenishmentApply::getRemainingAmount)
                .gt(BtgReplenishmentApply::getRemainingAmount, BigDecimal.ZERO)
                .orderByDesc(BtgReplenishmentApply::getId));
        return list.stream().map(this::toRepayableVo).toList();
    }

    private RepayableReplenishmentVO toRepayableVo(BtgReplenishmentApply a) {
        return RepayableReplenishmentVO.builder()
                .id(a.getId())
                .applyNo(a.getApplyNo())
                .approvedAmount(MoneyUtil.money(a.getApprovedAmount()))
                .repaidAmount(MoneyUtil.money(a.getRepaidAmount()))
                .pendingRepayAmount(MoneyUtil.money(a.getPendingRepayAmount()))
                .remainingAmount(MoneyUtil.money(a.getRemainingAmount()))
                .auditTime(a.getAuditTime())
                .transferScreenshotUrl(a.getTransferScreenshotUrl())
                .transferRemark(a.getTransferRemark())
                .build();
    }

    @Override
    public Page<RepayPendingBriefVO> pageMine(Long userId, long page, long size) {
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getUserId, userId)
                .orderByDesc(BtgReplenishmentRepayApply::getSubmitTime));
        Map<Long, BtgReplenishmentApply> applyMap = replenishmentByIds(collectReplenishIds(raw.getRecords()));
        Page<RepayPendingBriefVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> toRepayPendingBrief(e, applyMap.get(e.getReplenishApplyId())))
                .toList());
        return out;
    }

    @Override
    public Page<RepayPendingBriefVO> pagePendingForAdmin(long page, long size) {
        Page<BtgReplenishmentRepayApply> p = new Page<>(page, size);
        Page<BtgReplenishmentRepayApply> raw = repayApplyMapper.selectPage(p, new LambdaQueryWrapper<BtgReplenishmentRepayApply>()
                .eq(BtgReplenishmentRepayApply::getStatus, RepayStatusEnum.PENDING_AUDIT)
                .orderByAsc(BtgReplenishmentRepayApply::getSubmitTime));
        Map<Long, BtgReplenishmentApply> applyMap = replenishmentByIds(collectReplenishIds(raw.getRecords()));
        Page<RepayPendingBriefVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(e -> toRepayPendingBrief(e, applyMap.get(e.getReplenishApplyId())))
                .toList());
        return out;
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

    private static RepayPendingBriefVO toRepayPendingBrief(BtgReplenishmentRepayApply e, BtgReplenishmentApply apply) {
        RepayPendingBriefVO.RepayPendingBriefVOBuilder b = RepayPendingBriefVO.builder()
                .id(e.getId())
                .repayNo(e.getRepayNo())
                .status(e.getStatus() == null ? null : e.getStatus().getValue())
                .replenishApplyId(e.getReplenishApplyId());
        if (apply != null) {
            b.replenishApplyNo(apply.getApplyNo())
                    .replenishApprovedAmount(apply.getApprovedAmount())
                    .replenishRepaidAmount(apply.getRepaidAmount())
                    .replenishPendingRepayAmount(apply.getPendingRepayAmount())
                    .replenishRemainingAmount(apply.getRemainingAmount());
        }
        return b.build();
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

    @Override
    public RepayApplyVO getRepayDetailForUser(Long viewerUserId, Long repayApplyId) {
        BtgReplenishmentRepayApply e = repayApplyMapper.selectById(repayApplyId);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (!viewerUserId.equals(e.getUserId()) && !userService.isUpstreamOf(viewerUserId, e.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该归仓申请");
        }
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveForAdmin(Long repayApplyId, Long adminUserId, String remark) {
        BtgReplenishmentRepayApply repay = repayApplyMapper.selectById(repayApplyId);
        if (repay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (repay.getStatus() != RepayStatusEnum.PENDING_AUDIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可审核通过");
        }
        if (repay.getReplenishApplyId() == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectById(repay.getReplenishApplyId());
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        if (parent.getStatus() != ReplenishmentStatusEnum.APPROVED
                && parent.getStatus() != ReplenishmentStatusEnum.PARTIALLY_REPAID) {
            throw new BizException(ResultCode.CONFLICT, "关联补仓单状态异常");
        }
        BigDecimal r = MoneyUtil.money(repay.getRepayAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        if (pending.compareTo(r) < 0) {
            throw new BizException(ResultCode.CONFLICT, "待审核归仓金额数据异常");
        }
        BigDecimal newPending = MoneyUtil.money(pending.subtract(r));
        BigDecimal newRepaid = MoneyUtil.money(MoneyUtil.money(parent.getRepaidAmount()).add(r));
        BigDecimal approved = MoneyUtil.money(parent.getApprovedAmount());
        BigDecimal newRemaining = MoneyUtil.money(approved.subtract(newRepaid));

        repay.setStatus(RepayStatusEnum.APPROVED);
        repay.setAuditBy(adminUserId);
        repay.setAuditTime(LocalDateTime.now());
        repay.setAuditRemark(trimOrNull(remark));
        repayApplyMapper.updateById(repay);

        parent.setPendingRepayAmount(newPending);
        parent.setRepaidAmount(newRepaid);
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            parent.setRemainingAmount(MoneyUtil.money(null));
            parent.setStatus(ReplenishmentStatusEnum.FULLY_REPAID);
        } else {
            parent.setRemainingAmount(newRemaining);
            parent.setStatus(ReplenishmentStatusEnum.PARTIALLY_REPAID);
        }
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, repay.getId(), AuditAction.APPROVE, adminUserId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectForAdmin(Long repayApplyId, Long adminUserId, String remark) {
        BtgReplenishmentRepayApply repay = repayApplyMapper.selectById(repayApplyId);
        if (repay == null) {
            throw new BizException(ResultCode.NOT_FOUND, "归仓申请不存在");
        }
        if (repay.getStatus() != RepayStatusEnum.PENDING_AUDIT) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
        }
        if (repay.getReplenishApplyId() == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        BtgReplenishmentApply parent = replenishmentApplyMapper.selectById(repay.getReplenishApplyId());
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "关联补仓单不存在");
        }
        BigDecimal r = MoneyUtil.money(repay.getRepayAmount());
        BigDecimal pending = MoneyUtil.money(parent.getPendingRepayAmount());
        if (pending.compareTo(r) < 0) {
            throw new BizException(ResultCode.CONFLICT, "待审核归仓金额数据异常");
        }

        repay.setStatus(RepayStatusEnum.REJECTED);
        repay.setAuditBy(adminUserId);
        repay.setAuditTime(LocalDateTime.now());
        repay.setAuditRemark(trimOrNull(remark));
        repayApplyMapper.updateById(repay);

        parent.setPendingRepayAmount(MoneyUtil.money(pending.subtract(r)));
        replenishmentApplyMapper.updateById(parent);

        auditLogService.log(AuditBusinessType.REPLENISHMENT_REPAY, repay.getId(), AuditAction.REJECT, adminUserId, remark);
    }

    private static String trimOrNull(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        return remark.trim();
    }

    private static String nextRepayNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "WA" + ts + rnd;
    }

    private RepayApplyVO buildRepayVo(BtgReplenishmentRepayApply e, BtgUser user, BtgReplenishmentApply apply) {
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
                .updatedAt(e.getUpdatedAt());
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

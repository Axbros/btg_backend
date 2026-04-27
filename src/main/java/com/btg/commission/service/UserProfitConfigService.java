package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.dto.v1.ProfitConfigCreateRequest;
import com.btg.commission.dto.v1.ProfitConfigUpdateRequest;
import com.btg.commission.entity.UserProfitConfig;
import com.btg.commission.enums.CommissionModeEnum;
import com.btg.commission.enums.UserProfitConfigStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.mapper.UserProfitConfigMapper;
import com.btg.commission.vo.SelfUnderParentProfitConfigVo;
import com.btg.commission.vo.UserDetailViewerProfitConfigVo;
import com.btg.commission.vo.UserProfitConfigListItemVO;
import com.btg.commission.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserProfitConfigService {

    private final UserProfitConfigMapper userProfitConfigMapper;
    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserQualificationGateService userQualificationGateService;

    /**
     * 当前用户作为「子」，在直属上级处生效的子级总利润占比配置。
     */
    public UserProfitConfig findActiveForUserAsChild(Long childUserId) {
        BtgUser child = btgUserMapper.selectById(childUserId);
        if (child == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long parentId = child.getReferrerUserId();
        if (parentId == null || parentId == 0L) {
            throw new BizException(ResultCode.CONFLICT, "根用户无上级的分润配置");
        }
        UserProfitConfig cfg = userProfitConfigMapper.selectOne(new LambdaQueryWrapper<UserProfitConfig>()
                .eq(UserProfitConfig::getParentUserId, parentId)
                .eq(UserProfitConfig::getChildUserId, childUserId)
                .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                .last("LIMIT 1"));
        if (cfg == null) {
            throw new BizException(ResultCode.CONFLICT, "直属上级尚未为您配置分润比例");
        }
        return cfg;
    }

    /**
     * {@link #findActiveForUserAsChild(Long)} 并附带直属上级 {@link UserProfile#getExchangeUid()}（无资料行或未填则为 null）。
     */
    public SelfUnderParentProfitConfigVo findActiveForUserAsChildWithParentProfile(Long childUserId) {
        UserProfitConfig cfg = findActiveForUserAsChild(childUserId);
        UserProfile parentProfile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, cfg.getParentUserId())
                .last("LIMIT 1"));
        String parentExchangeUid = null;
        if (parentProfile != null && StringUtils.hasText(parentProfile.getExchangeUid())) {
            parentExchangeUid = parentProfile.getExchangeUid().trim();
        }
        return new SelfUnderParentProfitConfigVo(cfg, parentExchangeUid);
    }

    public List<UserProfitConfigListItemVO> listMyDirectChildrenConfigs(Long parentUserId) {
        return userProfitConfigMapper.selectList(new LambdaQueryWrapper<UserProfitConfig>()
                        .eq(UserProfitConfig::getParentUserId, parentUserId)
                        .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                        .orderByAsc(UserProfitConfig::getChildUserId))
                .stream()
                .map(UserProfitConfigService::toListItemVo)
                .toList();
    }

    /**
     * 当前查看者视角：目标用户所在「本人直属分支」相对总利润的子级线占比。
     * 即查看者与「从本人到目标路径上的直属下级」之间的 ACTIVE {@code child_profit_ratio}；
     * 查看者与目标同一人、查看者非目标上级、或该边上无有效配置时为 {@code null}。
     */
    public BigDecimal childLineProfitRatioForViewer(Long viewerUserId, Long targetUserId) {
        if (viewerUserId == null || targetUserId == null || Objects.equals(viewerUserId, targetUserId)) {
            return null;
        }
        BtgUser target = btgUserMapper.selectById(targetUserId);
        if (target == null) {
            return null;
        }
        Long branchChildId = directChildOnPathFromViewerToTarget(viewerUserId, target);
        if (branchChildId == null) {
            return null;
        }
        UserProfitConfig cfg = userProfitConfigMapper.selectOne(new LambdaQueryWrapper<UserProfitConfig>()
                .eq(UserProfitConfig::getParentUserId, viewerUserId)
                .eq(UserProfitConfig::getChildUserId, branchChildId)
                .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                .last("LIMIT 1"));
        if (cfg == null) {
            return null;
        }
        return effectiveChildProfitRatio(cfg);
    }

    /**
     * {@code GET /user/{target}} 用：与当前登录者相关的 ACTIVE 分润边。
     * <ul>
     *   <li>查看自己：直属上级 → 本人；</li>
     *   <li>查看他人：本人 → 从本人到目标路径上的直属子（与 {@link #childLineProfitRatioForViewer} 同源）。</li>
     * </ul>
     */
    public UserProfitConfig activeEdgeForUserDetailView(Long viewerUserId, Long targetUserId) {
        if (viewerUserId == null || targetUserId == null) {
            return null;
        }
        if (Objects.equals(viewerUserId, targetUserId)) {
            BtgUser target = btgUserMapper.selectById(targetUserId);
            if (target == null) {
                return null;
            }
            Long parentId = target.getReferrerUserId();
            if (parentId == null || parentId == 0L) {
                return null;
            }
            return selectActiveEdge(parentId, targetUserId);
        }
        BtgUser target = btgUserMapper.selectById(targetUserId);
        if (target == null) {
            return null;
        }
        Long branchChildId = directChildOnPathFromViewerToTarget(viewerUserId, target);
        if (branchChildId == null) {
            return null;
        }
        return selectActiveEdge(viewerUserId, branchChildId);
    }

    /**
     * 将 {@link #activeEdgeForUserDetailView} 结果转为 VO；无配置时为 null。
     * 仅老库字段 {@code child_profit_ratio} 时，兜底/不兜底展示均回落为该值。
     */
    public UserDetailViewerProfitConfigVo viewerProfitConfigForUserDetail(Long viewerUserId, Long targetUserId) {
        return toViewerProfitConfigVo(activeEdgeForUserDetailView(viewerUserId, targetUserId), viewerUserId);
    }

    private UserProfitConfig selectActiveEdge(Long parentUserId, Long childUserId) {
        return userProfitConfigMapper.selectOne(new LambdaQueryWrapper<UserProfitConfig>()
                .eq(UserProfitConfig::getParentUserId, parentUserId)
                .eq(UserProfitConfig::getChildUserId, childUserId)
                .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                .last("LIMIT 1"));
    }

    private UserDetailViewerProfitConfigVo toViewerProfitConfigVo(UserProfitConfig cfg, Long viewerUserId) {
        if (cfg == null) {
            return null;
        }
        BigDecimal legacy = cfg.getChildProfitRatio() == null ? null : MoneyUtil.profitRatio(cfg.getChildProfitRatio());
        BigDecimal g = cfg.getGuaranteeRatio() != null ? MoneyUtil.profitRatio(cfg.getGuaranteeRatio()) : legacy;
        BigDecimal n = cfg.getNonGuaranteeRatio() != null ? MoneyUtil.profitRatio(cfg.getNonGuaranteeRatio()) : legacy;
        BigDecimal maxG = parentAssignableGuaranteeRatioOrNull(viewerUserId);
        BigDecimal maxN = parentAssignableNonGuaranteeRatioOrNull(viewerUserId);
        return UserDetailViewerProfitConfigVo.builder()
                .guaranteeRatio(g)
                .nonGuaranteeRatio(n)
                .commissionMode(cfg.getCommissionMode())
                .commissionModeDesc(CommissionModeEnum.descriptionOrNull(cfg.getCommissionMode()))
                .maxAssignableChildGuaranteeRatio(maxG)
                .maxAssignableChildNonGuaranteeRatio(maxN)
                .build();
    }

    /**
     * 从 target 沿 {@code referrer_user_id} 上溯，找到第一个「上级为 viewer」的用户 id（即 viewer 在该路径上的直属子）。
     */
    private Long directChildOnPathFromViewerToTarget(Long viewerUserId, BtgUser target) {
        BtgUser cur = target;
        for (int i = 0; i < 10_000 && cur != null; i++) {
            Long ref = cur.getReferrerUserId();
            if (ref == null || ref == 0L) {
                return null;
            }
            if (ref.equals(viewerUserId)) {
                return cur.getId();
            }
            cur = btgUserMapper.selectById(ref);
        }
        return null;
    }

    /**
     * 本人作为父级时，兜底/不兜底两条边上可给下级的比例上限中较紧的一条（min），供旧版「单滑块」上限展示兼容。
     * 根为 1；非根为上级对自己 ACTIVE 配置上的 guarantee / non_guarantee（无新列时回落 child_profit_ratio）。
     */
    public BigDecimal parentAssignableRatioOrNull(Long parentUserId) {
        BigDecimal capG = parentAssignableGuaranteeRatioOrNull(parentUserId);
        BigDecimal capN = parentAssignableNonGuaranteeRatioOrNull(parentUserId);
        if (capG == null || capN == null) {
            return null;
        }
        return capG.compareTo(capN) <= 0 ? capG : capN;
    }

    public BigDecimal parentAssignableRatio(Long parentUserId) {
        BtgUser parent = btgUserMapper.selectById(parentUserId);
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "父级用户不存在");
        }
        BigDecimal r = parentAssignableRatioOrNull(parentUserId);
        if (r == null) {
            throw new BizException(ResultCode.CONFLICT, "父级在链路上未配置可分比例，无法为下级设比例");
        }
        return r;
    }

    /**
     * 查看者为目标用户上级链上的祖先时：查看者给「该分支上自己的直属子」配置子级总利润占比时允许的最大值（0～1）。
     * 与 {@link #parentAssignableRatio(Long)} 对查看者本人的上限一致；非下级链、或查看者无上級可分比例时为 null。
     */
    public BigDecimal maxAssignableChildProfitRatioForViewer(Long viewerUserId, Long targetUserId) {
        if (viewerUserId == null || targetUserId == null || Objects.equals(viewerUserId, targetUserId)) {
            return null;
        }
        BtgUser target = btgUserMapper.selectById(targetUserId);
        if (target == null) {
            return null;
        }
        Long branchChildId = directChildOnPathFromViewerToTarget(viewerUserId, target);
        if (branchChildId == null) {
            return null;
        }
        return parentAssignableRatioOrNull(viewerUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserProfitConfig create(Long parentUserId, ProfitConfigCreateRequest req) {
        if (req == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求体不能为空");
        }
        CommissionModeEnum mode = req.getCommissionMode();
        if (mode == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "commissionMode 无效");
        }
        Long childUserId = req.getChildUserId();
        assertDirectChild(parentUserId, childUserId);
        assertNonRootProfitEditor(parentUserId);
        userQualificationGateService.requireApprovedForFormalBusiness(parentUserId);
        userQualificationGateService.requireApprovedForFormalBusiness(childUserId);
        BigDecimal g = MoneyUtil.profitRatio(req.getGuaranteeRatio());
        BigDecimal n = MoneyUtil.profitRatio(req.getNonGuaranteeRatio());
        assertRatioAgainstParentCaps(parentUserId, g, n);

        deactivateExisting(parentUserId, childUserId);
        UserProfitConfig row = new UserProfitConfig();
        row.setParentUserId(parentUserId);
        row.setChildUserId(childUserId);
        row.setGuaranteeRatio(g);
        row.setNonGuaranteeRatio(n);
        row.setCommissionMode(mode.name());
        row.setChildProfitRatio(mode == CommissionModeEnum.NON_GUARANTEE ? n : g);
        row.setStatus(UserProfitConfigStatus.ACTIVE);
        row.setEffectiveTime(LocalDateTime.now());
        userProfitConfigMapper.insert(row);
        return row;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserProfitConfig updateById(Long id, Long parentUserId, ProfitConfigUpdateRequest req) {
        if (req == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求体不能为空");
        }
        CommissionModeEnum mode = req.getCommissionMode();
        if (mode == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "commissionMode 无效");
        }
        UserProfitConfig existing = userProfitConfigMapper.selectById(id);
        if (existing == null || !existing.getParentUserId().equals(parentUserId)) {
            throw new BizException(ResultCode.NOT_FOUND, "配置不存在");
        }
        assertDirectChild(parentUserId, existing.getChildUserId());
        assertNonRootProfitEditor(parentUserId);
        userQualificationGateService.requireApprovedForFormalBusiness(parentUserId);
        userQualificationGateService.requireApprovedForFormalBusiness(existing.getChildUserId());
        BigDecimal g = MoneyUtil.profitRatio(req.getGuaranteeRatio());
        BigDecimal n = MoneyUtil.profitRatio(req.getNonGuaranteeRatio());
        assertRatioAgainstParentCaps(parentUserId, g, n);

        existing.setGuaranteeRatio(g);
        existing.setNonGuaranteeRatio(n);
        existing.setCommissionMode(mode.name());
        existing.setChildProfitRatio(mode == CommissionModeEnum.NON_GUARANTEE ? n : g);
        existing.setEffectiveTime(LocalDateTime.now());
        userProfitConfigMapper.updateById(existing);
        return existing;
    }

    private void assertRatioAgainstParentCaps(Long parentUserId, BigDecimal guaranteeRatio, BigDecimal nonGuaranteeRatio) {
        BigDecimal capG = parentAssignableGuaranteeRatioOrNull(parentUserId);
        BigDecimal capN = parentAssignableNonGuaranteeRatioOrNull(parentUserId);
        if (capG == null || capN == null) {
            throw new BizException(ResultCode.CONFLICT, "父级在链路上未配置可分比例，无法为下级设比例");
        }
        if (guaranteeRatio.compareTo(capG) > 0) {
            throw new BizException(ResultCode.CONFLICT, "兜底比例不能超过上级给你的兜底比例");
        }
        if (nonGuaranteeRatio.compareTo(capN) > 0) {
            throw new BizException(ResultCode.CONFLICT, "不兜底比例不能超过上级给你的不兜底比例");
        }
    }

    /** 上级为「兜底」时子级可分比例上限；根为 1 */
    public BigDecimal parentAssignableGuaranteeRatioOrNull(Long parentUserId) {
        if (parentUserId == null) {
            return null;
        }
        BtgUser parent = btgUserMapper.selectById(parentUserId);
        if (parent == null) {
            return null;
        }
        if (Boolean.TRUE.equals(parent.getIsRoot()) || parent.getReferrerUserId() == null || parent.getReferrerUserId() == 0L) {
            return MoneyUtil.profitRatio(BigDecimal.ONE);
        }
        UserProfitConfig edge = userProfitConfigMapper.selectOne(new LambdaQueryWrapper<UserProfitConfig>()
                .eq(UserProfitConfig::getParentUserId, parent.getReferrerUserId())
                .eq(UserProfitConfig::getChildUserId, parentUserId)
                .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                .last("LIMIT 1"));
        if (edge == null) {
            return null;
        }
        return MoneyUtil.profitRatio(edgeGuaranteeRatio(edge));
    }

    /** 上级为「不兜底」时子级可分比例上限；根为 1 */
    public BigDecimal parentAssignableNonGuaranteeRatioOrNull(Long parentUserId) {
        if (parentUserId == null) {
            return null;
        }
        BtgUser parent = btgUserMapper.selectById(parentUserId);
        if (parent == null) {
            return null;
        }
        if (Boolean.TRUE.equals(parent.getIsRoot()) || parent.getReferrerUserId() == null || parent.getReferrerUserId() == 0L) {
            return MoneyUtil.profitRatio(BigDecimal.ONE);
        }
        UserProfitConfig edge = userProfitConfigMapper.selectOne(new LambdaQueryWrapper<UserProfitConfig>()
                .eq(UserProfitConfig::getParentUserId, parent.getReferrerUserId())
                .eq(UserProfitConfig::getChildUserId, parentUserId)
                .eq(UserProfitConfig::getStatus, UserProfitConfigStatus.ACTIVE)
                .last("LIMIT 1"));
        if (edge == null) {
            return null;
        }
        return MoneyUtil.profitRatio(edgeNonGuaranteeRatio(edge));
    }

    private static BigDecimal edgeGuaranteeRatio(UserProfitConfig edge) {
        if (edge.getGuaranteeRatio() != null) {
            return edge.getGuaranteeRatio();
        }
        return edge.getChildProfitRatio();
    }

    private static BigDecimal edgeNonGuaranteeRatio(UserProfitConfig edge) {
        if (edge.getNonGuaranteeRatio() != null) {
            return edge.getNonGuaranteeRatio();
        }
        return edge.getChildProfitRatio();
    }

    private static BigDecimal effectiveChildProfitRatio(UserProfitConfig cfg) {
        CommissionModeEnum mode = CommissionModeEnum.fromCode(cfg.getCommissionMode());
        if (mode == null) {
            mode = CommissionModeEnum.GUARANTEE;
        }
        if (mode == CommissionModeEnum.NON_GUARANTEE) {
            return MoneyUtil.profitRatio(edgeNonGuaranteeRatio(cfg));
        }
        return MoneyUtil.profitRatio(edgeGuaranteeRatio(cfg));
    }

    private static UserProfitConfigListItemVO toListItemVo(UserProfitConfig e) {
        return UserProfitConfigListItemVO.builder()
                .id(e.getId())
                .parentUserId(e.getParentUserId())
                .childUserId(e.getChildUserId())
                .childProfitRatio(e.getChildProfitRatio() == null ? null : MoneyUtil.profitRatio(e.getChildProfitRatio()))
                .guaranteeRatio(e.getGuaranteeRatio() == null ? null : MoneyUtil.profitRatio(e.getGuaranteeRatio()))
                .nonGuaranteeRatio(e.getNonGuaranteeRatio() == null ? null : MoneyUtil.profitRatio(e.getNonGuaranteeRatio()))
                .commissionMode(e.getCommissionMode())
                .commissionModeDesc(CommissionModeEnum.descriptionOrNull(e.getCommissionMode()))
                .status(e.getStatus())
                .effectiveTime(e.getEffectiveTime())
                .expireTime(e.getExpireTime())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private void assertDirectChild(Long parentUserId, Long childUserId) {
        BtgUser child = btgUserMapper.selectById(childUserId);
        if (child == null) {
            throw new BizException(ResultCode.NOT_FOUND, "下级用户不存在");
        }
        if (child.getReferrerUserId() == null || !child.getReferrerUserId().equals(parentUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅直属上级可为该下级配置或修改分润比例");
        }
    }

    /** 产品规则：根用户不参与下级分润比例绑定/调整，仅非根直属上级可操作。 */
    private void assertNonRootProfitEditor(Long parentUserId) {
        BtgUser parent = btgUserMapper.selectById(parentUserId);
        if (parent == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
//        if (Boolean.TRUE.equals(parent.getIsRoot())) {
//            throw new BizException(ResultCode.FORBIDDEN, "根用户不能为下级调整分润比例");
//        }
    }

    private void deactivateExisting(Long parentUserId, Long childUserId) {
        userProfitConfigMapper.update(null, new LambdaUpdateWrapper<UserProfitConfig>()
                .set(UserProfitConfig::getDeletedAt, LocalDateTime.now())
                .eq(UserProfitConfig::getParentUserId, parentUserId)
                .eq(UserProfitConfig::getChildUserId, childUserId)
                .isNull(UserProfitConfig::getDeletedAt));
    }
}

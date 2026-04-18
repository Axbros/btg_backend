package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.QualificationStatusEnum;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.vo.TeamMemberTreeRow;
import com.btg.commission.vo.TeamMemberTreeVo;
import com.btg.commission.vo.UserDetailProfileVo;
import com.btg.commission.vo.UserDetailUserVo;
import com.btg.commission.vo.UserDetailVo;
import com.btg.commission.vo.UserMeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    /** 防止脏推荐环导致死循环；正常团队深度远小于此值 */
    private static final int MAX_TEAM_TREE_DEPTH = 512;

    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserProfitConfigService userProfitConfigService;
    private final UserProfileService userProfileService;

    public UserMeVo me(Long userId) {
        BtgUser u = btgUserMapper.selectById(userId);
        if (u == null) {
            return null;
        }
        String referrerNickname = referrerNicknameOf(u.getReferrerUserId());

        return UserMeVo.builder()
                .id(u.getId())
                .mobile(u.getMobile())
                .status(u.getStatus())
                .isRoot(u.getIsRoot())
                .referrerUserId(u.getReferrerUserId())
                .ancestorPath(u.getAncestorPath())
                .invitationCode(invitationCodeForApi(u))
                .nickname(u.getNickname())
                .referrerNickname(referrerNickname)
                .profile(userProfileService.buildProfileVo(u))
                .build();
    }

    /**
     * 当前用户下级的完整树：仅包含本人以下节点，根列表为直属下级，各自递归带 children。
     * 数据来源为推荐链递归（与 {@code ancestor_path} 列是否干净无关）；列仍由注册逻辑写入供其它场景使用。
     */
    public List<TeamMemberTreeVo> treeDescendants(Long currentUserId) {
        BtgUser self = btgUserMapper.selectById(currentUserId);
        if (self == null) {
            return Collections.emptyList();
        }
        List<TeamMemberTreeRow> rows = loadAllDescendantRows(currentUserId);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, TeamMemberTreeVo> nodes = new HashMap<>(rows.size() * 2);
        for (TeamMemberTreeRow r : rows) {
            nodes.put(r.getId(), TeamMemberTreeVo.builder()
                    .id(r.getId())
                    .nickname(r.getNickname())
                    .mobile(r.getMobile())
//                    .status(r.getStatus())
                    .children(new ArrayList<>())
                    .build());
        }
        List<Long> treeUserIds = new ArrayList<>(nodes.keySet());
        if (!treeUserIds.isEmpty()) {
            Map<Long, UserProfile> profileByUserId = userProfileMapper
                    .selectList(new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, treeUserIds))
                    .stream()
                    .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));
            for (TeamMemberTreeVo node : nodes.values()) {
                UserProfile pr = profileByUserId.get(node.getId());
                if (pr != null) {
                    node.setQualificationStatus(pr.getQualificationStatus() != null
                            ? pr.getQualificationStatus()
                            : QualificationStatusEnum.PENDING);
                    node.setQualificationAuditTime(pr.getQualificationAuditTime());
//                    node.setQualificationAuditRemark(pr.getQualificationAuditRemark());
                } else {
                    node.setQualificationStatus(QualificationStatusEnum.PENDING);
                }
            }
        }
        List<TeamMemberTreeVo> roots = new ArrayList<>();
        for (TeamMemberTreeRow r : rows) {
            TeamMemberTreeVo node = nodes.get(r.getId());
            if (currentUserId.equals(r.getReferrerUserId())) {
                roots.add(node);
            } else {
                TeamMemberTreeVo parent = nodes.get(r.getReferrerUserId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        sortTreeChildren(roots);
        return roots;
    }

    /**
     * 按 {@code referrer_user_id} 广度优先拉平整棵下级（不含本人），兼容无 CTE 的 MySQL 5.7。
     */
    private List<TeamMemberTreeRow> loadAllDescendantRows(Long viewerUserId) {
        Map<Long, TeamMemberTreeRow> byId = new LinkedHashMap<>();
        List<Long> frontier = new ArrayList<>();
        frontier.add(viewerUserId);
        int depth = 0;
        while (!frontier.isEmpty() && depth++ < MAX_TEAM_TREE_DEPTH) {
            List<TeamMemberTreeRow> batch = btgUserMapper.selectTeamMemberRowsByReferrerIds(frontier);
            List<Long> next = new ArrayList<>();
            for (TeamMemberTreeRow r : batch) {
                if (r.getId() == null || viewerUserId.equals(r.getId())) {
                    continue;
                }
                if (byId.putIfAbsent(r.getId(), r) == null) {
                    next.add(r.getId());
                }
            }
            frontier = next;
        }
        List<TeamMemberTreeRow> out = new ArrayList<>(byId.values());
        out.sort(Comparator.comparing(TeamMemberTreeRow::getId, Comparator.nullsLast(Long::compareTo)));
        return out;
    }

    private void sortTreeChildren(List<TeamMemberTreeVo> level) {
        if (level == null || level.isEmpty()) {
            return;
        }
        level.sort(Comparator.comparing(TeamMemberTreeVo::getId, Comparator.nullsLast(Long::compareTo)));
        for (TeamMemberTreeVo n : level) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortTreeChildren(n.getChildren());
            }
        }
    }

    public long countDirectChildren(Long referrerUserId) {
        return btgUserMapper.countDirectChildren(referrerUserId);
    }

    public long countAllDescendants(Long userId) {
        BtgUser self = btgUserMapper.selectById(userId);
        if (self == null) {
            return 0;
        }
        return loadAllDescendantRows(userId).size();
    }

    /**
     * 本人以下（不含本人）全部下级用户 id，用于团队补仓/归仓列表等；无下级时为空列表。
     * 与团队树一致：仅按 {@code referrer_user_id} 推荐链展开，不依赖 {@code ancestor_path} 是否干净。
     */
    public List<Long> listDescendantUserIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        if (btgUserMapper.selectById(userId) == null) {
            return Collections.emptyList();
        }
        return loadAllDescendantRows(userId).stream()
                .map(TeamMemberTreeRow::getId)
                .collect(Collectors.toList());
    }

    /**
     * 是否为 target 的直属上级或任意上级：沿 {@code referrer_user_id} 链向上判定，
     * 不依赖 {@code ancestor_path}（避免历史脏 path 导致误判）。
     */
    public boolean isUpstreamOf(Long upstreamUserId, Long targetUserId) {
        if (upstreamUserId == null || targetUserId == null) {
            return false;
        }
        if (upstreamUserId.equals(targetUserId)) {
            return false;
        }
        BtgUser target = btgUserMapper.selectById(targetUserId);
        if (target == null) {
            return false;
        }
        Long cur = target.getReferrerUserId();
        Set<Long> seen = new HashSet<>();
        int guard = 0;
        while (cur != null && seen.add(cur) && guard++ < 512) {
            if (cur.equals(upstreamUserId)) {
                return true;
            }
            BtgUser next = btgUserMapper.selectById(cur);
            if (next == null) {
                break;
            }
            cur = next.getReferrerUserId();
        }
        return false;
    }

    public UserMeVo getById(Long userId) {
        return me(userId);
    }

    public UserDetailVo getDetailById(Long targetUserId, Long viewerUserId) {
        BtgUser u = btgUserMapper.selectById(targetUserId);
        if (u == null) {
            return null;
        }
        assertCanViewUserDetail(viewerUserId, targetUserId);

        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        boolean viewerIsRoot = viewer != null && Boolean.TRUE.equals(viewer.getIsRoot());
        boolean selfView = Objects.equals(viewerUserId, targetUserId);

        String referrerNickname = referrerNicknameOf(u.getReferrerUserId());
        UserDetailUserVo userVo = UserDetailUserVo.builder()
                .id(u.getId())
                .mobile(u.getMobile())
                .status(u.getStatus())
                .isRoot(u.getIsRoot())
                .referrerUserId(u.getReferrerUserId())
                .ancestorPath(u.getAncestorPath())
                .invitationCode(invitationCodeForApi(u))
                .nickname(u.getNickname())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .referrerNickname(referrerNickname)
                .build();

        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, targetUserId)
                .last("LIMIT 1"));
        UserDetailProfileVo profileVo = toUserDetailProfileVo(profile, viewerIsRoot);
        boolean directParent = Objects.equals(u.getReferrerUserId(), viewerUserId);
        BigDecimal childLineProfitRatio = null;
        BigDecimal maxAssignableChildProfitRatio = null;
        if (selfView || viewerIsRoot || directParent) {
            childLineProfitRatio = userProfitConfigService.childLineProfitRatioForViewer(viewerUserId, targetUserId);
            maxAssignableChildProfitRatio = userProfitConfigService.maxAssignableChildProfitRatioForViewer(viewerUserId, targetUserId);
        }

        return UserDetailVo.builder()
                .user(userVo)
                .profile(profileVo)
                .childLineProfitRatio(childLineProfitRatio)
                .maxAssignableChildProfitRatio(maxAssignableChildProfitRatio)
                .build();
    }

    private static UserDetailProfileVo toUserDetailProfileVo(UserProfile p, boolean viewerIsRoot) {
        if (p == null) {
            return null;
        }
        return UserDetailProfileVo.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .realName(p.getRealName())
                .idCardNo(p.getIdCardNo())
                .idCardFrontUrl(p.getIdCardFrontUrl())
                .idCardBackUrl(p.getIdCardBackUrl())
                .facePhotoUrl(p.getFacePhotoUrl())
                .serverName(p.getServerName())
                .tradingAccountId(p.getTradingAccountId())
                .tradingAccountPassword(viewerIsRoot ? p.getTradingAccountPassword() : null)
                .exchangeUid(p.getExchangeUid())
                .walletName(p.getWalletName())
                .walletAddress(p.getWalletAddress())
                .principalAmount(p.getPrincipalAmount())
                .qualificationStatus(p.getQualificationStatus())
                .qualificationAuditTime(p.getQualificationAuditTime())
                .qualificationAuditBy(p.getQualificationAuditBy())
                .qualificationAuditRemark(p.getQualificationAuditRemark())
                .qualificationSubmitCount(p.getQualificationSubmitCount())
                .qualificationLastSubmitTime(p.getQualificationLastSubmitTime())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private void assertCanViewUserDetail(Long viewerUserId, Long targetUserId) {
        if (viewerUserId == null || targetUserId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "参数无效");
        }
        if (viewerUserId.equals(targetUserId)) {
            return;
        }
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        if (viewer != null && Boolean.TRUE.equals(viewer.getIsRoot())) {
            return;
        }
        if (isUpstreamOf(viewerUserId, targetUserId)) {
            return;
        }
        throw new BizException(ResultCode.FORBIDDEN, "无权查看该用户");
    }

    /** 仅审核通过（{@link UserStatus#NORMAL}）的用户对外返回邀请码，否则为 null（库内仍保留，待通过后展示）。 */
    private static String invitationCodeForApi(BtgUser u) {
        if (u == null || !UserStatus.canInviteOthers(u.getStatus())) {
            return null;
        }
        return u.getInvitationCode();
    }

    private String referrerNicknameOf(Long referrerUserId) {
        if (referrerUserId == null || referrerUserId == 0L) {
            return null;
        }
        BtgUser ref = btgUserMapper.selectById(referrerUserId);
        if (ref == null) {
            return null;
        }
        String nick = ref.getNickname();
        if (nick != null && !nick.trim().isEmpty()) {
            return nick.trim();
        }
        String mobile = ref.getMobile();
        if (mobile != null && !mobile.trim().isEmpty()) {
            return mobile.trim();
        }
        return null;
    }

}

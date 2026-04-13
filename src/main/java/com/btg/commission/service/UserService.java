package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.util.AncestorPathUtil;
import com.btg.commission.util.UserProfileBitgetHelper;
import com.btg.commission.vo.TeamMemberTreeRow;
import com.btg.commission.vo.TeamMemberTreeVo;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

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

    @Transactional(rollbackFor = Exception.class)
    public void approveDirectChildProfile(Long parentUserId, Long childUserId) {
        BtgUser child = btgUserMapper.selectById(childUserId);
        if (child == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!parentUserId.equals(child.getReferrerUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅直属上级可审核");
        }
        if (child.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可审核通过");
        }
        BtgUser patch = new BtgUser();
        patch.setId(childUserId);
        patch.setStatus(UserStatus.NORMAL);
        btgUserMapper.updateById(patch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectDirectChildProfile(Long parentUserId, Long childUserId) {
        BtgUser child = btgUserMapper.selectById(childUserId);
        if (child == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!parentUserId.equals(child.getReferrerUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅直属上级可审核");
        }
        if (child.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new BizException(ResultCode.CONFLICT, "当前状态不可拒绝");
        }
        BtgUser patch = new BtgUser();
        patch.setId(childUserId);
        patch.setStatus(UserStatus.PROFILE_INCOMPLETE);
        btgUserMapper.updateById(patch);
    }

    /**
     * 当前用户下级的完整树：仅包含本人以下节点，根列表为直属下级，各自递归带 children。
     */
    public List<TeamMemberTreeVo> treeDescendants(Long currentUserId) {
        BtgUser self = btgUserMapper.selectById(currentUserId);
        if (self == null) {
            return Collections.emptyList();
        }
        String prefix = AncestorPathUtil.descendantPathPrefix(self);
        List<TeamMemberTreeRow> rows = btgUserMapper.selectDescendantsForTree(prefix);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, TeamMemberTreeVo> nodes = new HashMap<>(rows.size() * 2);
        for (TeamMemberTreeRow r : rows) {
            nodes.put(r.getId(), TeamMemberTreeVo.builder()
                    .id(r.getId())
                    .nickname(r.getNickname())
                    .status(r.getStatus())
                    .children(new ArrayList<>())
                    .build());
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
        return btgUserMapper.countAllDescendants(AncestorPathUtil.descendantPathPrefix(self));
    }

    /**
     * 是否为 target 的直属上级或任意上级（根据 {@code ancestor_path} 与 {@code referrer_user_id} 判断）。
     */
    /**
     * 本人以下（不含本人）全部下级用户 id，用于团队补仓/归仓列表等；无下级时为空列表。
     */
    public List<Long> listDescendantUserIds(Long userId) {
        BtgUser self = btgUserMapper.selectById(userId);
        if (self == null) {
            return Collections.emptyList();
        }
        String prefix = AncestorPathUtil.descendantPathPrefix(self);
        return btgUserMapper.selectList(new LambdaQueryWrapper<BtgUser>()
                        .likeRight(BtgUser::getAncestorPath, prefix))
                .stream()
                .map(BtgUser::getId)
                .collect(Collectors.toList());
    }

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
        if (upstreamUserId.equals(target.getReferrerUserId())) {
            return true;
        }
        String path = target.getAncestorPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String seg : path.split("/")) {
            if (seg.isEmpty()) {
                continue;
            }
            try {
                if (Long.parseLong(seg) == upstreamUserId) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
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
        if (profile != null) {
            UserProfileBitgetHelper.applyPresentation(profile);
        }

        BigDecimal childLineProfitRatio = userProfitConfigService.childLineProfitRatioForViewer(viewerUserId, targetUserId);
        BigDecimal maxAssignableChildProfitRatio = userProfitConfigService.maxAssignableChildProfitRatioForViewer(viewerUserId, targetUserId);

        return UserDetailVo.builder()
                .user(userVo)
                .profile(profile)
                .childLineProfitRatio(childLineProfitRatio)
                .maxAssignableChildProfitRatio(maxAssignableChildProfitRatio)
                .build();
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

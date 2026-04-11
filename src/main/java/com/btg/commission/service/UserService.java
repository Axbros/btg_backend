package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.util.AncestorPathUtil;
import com.btg.commission.vo.PageVo;
import com.btg.commission.vo.TeamMemberBriefVo;
import com.btg.commission.vo.UserDetailUserVo;
import com.btg.commission.vo.UserDetailVo;
import com.btg.commission.vo.UserMeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserProfitConfigService userProfitConfigService;

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
                .invitationCode(u.getInvitationCode())
                .nickname(u.getNickname())
                .referrerNickname(referrerNickname)
                .build();
    }

    public PageVo<TeamMemberBriefVo> pageDirectChildren(Long referrerUserId, long page, long pageSize) {
        long p = Math.max(1L, page);
        long s = Math.min(MAX_PAGE_SIZE, Math.max(1L, pageSize));
        Page<TeamMemberBriefVo> mp = new Page<>(p, s);
        Page<TeamMemberBriefVo> result = btgUserMapper.selectDirectChildrenPage(mp, referrerUserId);
        return toPageVo(result);
    }

    public PageVo<TeamMemberBriefVo> pageAllDescendants(Long userId, long page, long pageSize) {
        long p = Math.max(1L, page);
        long s = Math.min(MAX_PAGE_SIZE, Math.max(1L, pageSize));
        BtgUser self = btgUserMapper.selectById(userId);
        if (self == null) {
            return PageVo.<TeamMemberBriefVo>builder()
                    .records(Collections.emptyList())
                    .total(0)
                    .page(p)
                    .pageSize(s)
                    .build();
        }
        String prefix = AncestorPathUtil.descendantPathPrefix(self);
        Page<TeamMemberBriefVo> mp = new Page<>(p, s);
        Page<TeamMemberBriefVo> result = btgUserMapper.selectAllDescendantsPage(mp, prefix);
        return toPageVo(result);
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
                .invitationCode(u.getInvitationCode())
                .nickname(u.getNickname())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .referrerNickname(referrerNickname)
                .build();

        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, targetUserId)
                .last("LIMIT 1"));

        BigDecimal childLineProfitRatio = userProfitConfigService.childLineProfitRatioForViewer(viewerUserId, targetUserId);

        return UserDetailVo.builder()
                .user(userVo)
                .profile(profile)
                .childLineProfitRatio(childLineProfitRatio)
                .build();
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

    private PageVo<TeamMemberBriefVo> toPageVo(Page<TeamMemberBriefVo> result) {
        return PageVo.<TeamMemberBriefVo>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .page(result.getCurrent())
                .pageSize(result.getSize())
                .build();
    }

}

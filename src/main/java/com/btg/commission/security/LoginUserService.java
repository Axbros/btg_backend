package com.btg.commission.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.QualificationStatusEnum;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserService implements UserDetailsService {

    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserDetails loadUserByUsername(String mobile) throws UsernameNotFoundException {
        BtgUser u = btgUserMapper.selectOne(new LambdaQueryWrapper<BtgUser>()
                .eq(BtgUser::getMobile, mobile)
                .last("LIMIT 1"));
        if (u == null) {
            throw new UsernameNotFoundException("user not found");
        }
        if (!UserStatus.canAuthenticate(u.getStatus())) {
            throw new UsernameNotFoundException("user disabled");
        }
        boolean admin = Boolean.TRUE.equals(u.getIsRoot());
        QualificationStatusEnum qual = resolveQualificationStatus(u);
        return new LoginUser(u.getId(), u.getMobile(), u.getPasswordHash(), admin, u.getStatus(), qual);
    }

    public LoginUser loadByUserId(Long userId) {
        BtgUser u = btgUserMapper.selectById(userId);
        if (u == null || !UserStatus.canAuthenticate(u.getStatus())) {
            throw new UsernameNotFoundException("user not found");
        }
        boolean admin = Boolean.TRUE.equals(u.getIsRoot());
        QualificationStatusEnum qual = resolveQualificationStatus(u);
        return new LoginUser(u.getId(), u.getMobile(), u.getPasswordHash(), admin, u.getStatus(), qual);
    }

    private QualificationStatusEnum resolveQualificationStatus(BtgUser u) {
        if (Boolean.TRUE.equals(u.getIsRoot())) {
            return QualificationStatusEnum.APPROVED;
        }
        UserProfile p = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, u.getId())
                .last("LIMIT 1"));
        if (p == null || p.getQualificationStatus() == null) {
            return QualificationStatusEnum.APPROVED;
        }
        return p.getQualificationStatus();
    }
}

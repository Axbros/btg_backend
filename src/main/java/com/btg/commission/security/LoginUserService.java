package com.btg.commission.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.BtgUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserService implements UserDetailsService {

    private final BtgUserMapper btgUserMapper;

    @Override
    public UserDetails loadUserByUsername(String mobile) throws UsernameNotFoundException {
        BtgUser u = btgUserMapper.selectOne(new LambdaQueryWrapper<BtgUser>()
                .eq(BtgUser::getMobile, mobile)
                .last("LIMIT 1"));
        if (u == null) {
            throw new UsernameNotFoundException("user not found");
        }
        if (u.getStatus() != UserStatus.NORMAL) {
            throw new UsernameNotFoundException("user disabled");
        }
        boolean admin = Boolean.TRUE.equals(u.getIsRoot());
        return new LoginUser(u.getId(), u.getMobile(), u.getPasswordHash(), admin);
    }

    public LoginUser loadByUserId(Long userId) {
        BtgUser u = btgUserMapper.selectById(userId);
        if (u == null || u.getStatus() != UserStatus.NORMAL) {
            throw new UsernameNotFoundException("user not found");
        }
        boolean admin = Boolean.TRUE.equals(u.getIsRoot());
        return new LoginUser(u.getId(), u.getMobile(), u.getPasswordHash(), admin);
    }
}

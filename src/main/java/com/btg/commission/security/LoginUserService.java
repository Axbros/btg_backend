package com.btg.commission.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.SysUser;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String mobile) throws UsernameNotFoundException {
        SysUser u = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getMobile, mobile)
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
        SysUser u = sysUserMapper.selectById(userId);
        if (u == null || u.getStatus() != UserStatus.NORMAL) {
            throw new UsernameNotFoundException("user not found");
        }
        boolean admin = Boolean.TRUE.equals(u.getIsRoot());
        return new LoginUser(u.getId(), u.getMobile(), u.getPasswordHash(), admin);
    }
}

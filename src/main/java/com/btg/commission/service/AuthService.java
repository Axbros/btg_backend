package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.config.JwtProperties;
import com.btg.commission.dto.auth.LoginRequest;
import com.btg.commission.dto.auth.RegisterRequest;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.security.JwtTokenProvider;
import com.btg.commission.util.AncestorPathUtil;
import com.btg.commission.util.InviteCodeGenerator;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest req) {
        Long cnt = btgUserMapper.selectCount(new LambdaQueryWrapper<BtgUser>().eq(BtgUser::getMobile, req.getMobile()));
        if (cnt != null && cnt > 0) {
            throw new BizException(ResultCode.CONFLICT, "mobile already registered");
        }
        String code = req.getInvitationCode() == null ? "" : req.getInvitationCode().trim();
        BtgUser referrer;
        if (!StringUtils.hasText(code)) {
            referrer = btgUserMapper.selectOne(new LambdaQueryWrapper<BtgUser>()
                    .eq(BtgUser::getIsRoot, true)
                    .last("LIMIT 1"));
            if (referrer == null) {
                throw new BizException(ResultCode.CONFLICT, "系统未初始化根用户，无法注册");
            }
        } else {
            referrer = btgUserMapper.selectOne(new LambdaQueryWrapper<BtgUser>()
                    .eq(BtgUser::getInvitationCode, code)
                    .last("LIMIT 1"));
            if (referrer == null) {
                throw new BizException(ResultCode.NOT_FOUND, "邀请码无效");
            }
        }
        if (referrer.getStatus() != UserStatus.NORMAL) {
            throw new BizException(ResultCode.CONFLICT, "referrer disabled");
        }

        BtgUser user = new BtgUser();
        user.setMobile(req.getMobile());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setStatus(UserStatus.NORMAL);
        user.setIsRoot(false);
        user.setReferrerUserId(referrer.getId());
        user.setAncestorPath(AncestorPathUtil.buildChildAncestorPath(referrer));
        user.setInvitationCode(generateUniqueInvitationCode());
        user.setNickname(null);
        btgUserMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setPrincipalAmount(MoneyUtil.money(BigDecimal.ZERO));
        userProfileMapper.insert(profile);
    }

    private String generateUniqueInvitationCode() {
        for (int i = 0; i < 20; i++) {
            String code = "INV" + InviteCodeGenerator.random(6);
            Long c = btgUserMapper.selectCount(new LambdaQueryWrapper<BtgUser>().eq(BtgUser::getInvitationCode, code));
            if (c == null || c == 0) {
                return code;
            }
        }
        throw new BizException(ResultCode.INTERNAL_ERROR, "failed to allocate invitation code");
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getMobile(), req.getPassword()));
        BtgUser u = btgUserMapper.selectOne(new LambdaQueryWrapper<BtgUser>()
                .eq(BtgUser::getMobile, req.getMobile())
                .last("LIMIT 1"));
        if (u == null || u.getStatus() != UserStatus.NORMAL) {
            throw new BizException(ResultCode.UNAUTHORIZED, "invalid credentials");
        }
        boolean admin = Boolean.TRUE.equals(u.getIsRoot());
        String token = jwtTokenProvider.createToken(u.getId(), u.getMobile(), admin);

        return new TokenResponse(token, jwtProperties.getExpirationMs());
    }
}

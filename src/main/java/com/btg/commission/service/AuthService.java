package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.config.JwtProperties;
import com.btg.commission.dto.auth.LoginRequest;
import com.btg.commission.dto.auth.RegisterRequest;
import com.btg.commission.entity.SysUser;
import com.btg.commission.entity.UserAccountSummary;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.KycStatus;
import com.btg.commission.enums.UserStatus;
import com.btg.commission.mapper.SysUserMapper;
import com.btg.commission.mapper.UserAccountSummaryMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.security.JwtTokenProvider;
import com.btg.commission.util.AncestorPathUtil;
import com.btg.commission.util.InviteCodeGenerator;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserAccountSummaryMapper userAccountSummaryMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest req) {
        Long cnt = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getMobile, req.getMobile()));
        if (cnt != null && cnt > 0) {
            throw new BizException(ResultCode.CONFLICT, "mobile already registered");
        }
        String code = req.getInvitationCode() == null ? "" : req.getInvitationCode().trim();
        if (!StringUtils.hasText(code)) {
            throw new BizException(ResultCode.BAD_REQUEST, "邀请码不能为空");
        }
        SysUser referrer = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getInvitationCode, code)
                .last("LIMIT 1"));
        if (referrer == null) {
            throw new BizException(ResultCode.NOT_FOUND, "邀请码无效");
        }
        if (referrer.getStatus() != UserStatus.NORMAL) {
            throw new BizException(ResultCode.CONFLICT, "referrer disabled");
        }

        SysUser user = new SysUser();
        user.setMobile(req.getMobile());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setLoginSalt(null);
        user.setStatus(UserStatus.NORMAL);
        user.setIsRoot(false);
        user.setReferrerUserId(referrer.getId());
        user.setAncestorPath(AncestorPathUtil.buildChildAncestorPath(referrer));
        user.setInvitationCode(generateUniqueInvitationCode());
        user.setNickname(null);
        sysUserMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setKycStatus(KycStatus.NOT_SUBMITTED);
        profile.setPrincipalAmount(MoneyUtil.money(BigDecimal.ZERO));
        userProfileMapper.insert(profile);

        UserAccountSummary sum = new UserAccountSummary();
        sum.setUserId(user.getId());
        sum.setTotalProfitAmount(MoneyUtil.money(BigDecimal.ZERO));
        sum.setTotalCommissionOutAmount(MoneyUtil.money(BigDecimal.ZERO));
        sum.setTotalCommissionInAmount(MoneyUtil.money(BigDecimal.ZERO));
        sum.setPendingCommissionOutAmount(MoneyUtil.money(BigDecimal.ZERO));
        sum.setPendingCommissionInAmount(MoneyUtil.money(BigDecimal.ZERO));
        userAccountSummaryMapper.insert(sum);
    }

    private String generateUniqueInvitationCode() {
        for (int i = 0; i < 20; i++) {
            String code = "INV" + InviteCodeGenerator.random(6);
            Long c = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getInvitationCode, code));
            if (c == null || c == 0) {
                return code;
            }
        }
        throw new BizException(ResultCode.INTERNAL_ERROR, "failed to allocate invitation code");
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenResponse login(LoginRequest req, HttpServletRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getMobile(), req.getPassword()));
        SysUser u = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getMobile, req.getMobile())
                .last("LIMIT 1"));
        if (u == null || u.getStatus() != UserStatus.NORMAL) {
            throw new BizException(ResultCode.UNAUTHORIZED, "invalid credentials");
        }
        boolean admin = Boolean.TRUE.equals(u.getIsRoot());
        String token = jwtTokenProvider.createToken(u.getId(), u.getMobile(), admin);

        SysUser patch = new SysUser();
        patch.setId(u.getId());
        patch.setLastLoginTime(LocalDateTime.now());
        patch.setLastLoginIp(clientIp(request));
        sysUserMapper.updateById(patch);

        return new TokenResponse(token, jwtProperties.getExpirationMs());
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

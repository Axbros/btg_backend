package com.btg.commission.security;

import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser requireLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser user)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "login required");
        }
        return user;
    }

    public static Long requireUserId() {
        return requireLoginUser().getUserId();
    }

    /**
     * 业务上「系统管理员」= 根用户：{@link LoginUser#isAdmin()} 在登录/鉴权时由 {@code btg_user.is_root} 写入。
     * 与 {@link com.btg.commission.config.SecurityConfig} 中 {@code ${btg.api.base-path}/admin/**}.hasRole("ADMIN")} 一致；
     * 资格审核等敏感操作应在 Service 内再查库校验 {@code is_root}（见 {@code UserQualificationServiceImpl}）。
     */
    public static LoginUser requireRootUser() {
        LoginUser u = requireLoginUser();
        if (!u.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN, "仅根用户（系统管理员）可访问该功能");
        }
        return u;
    }
}

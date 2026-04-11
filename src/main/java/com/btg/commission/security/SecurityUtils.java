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
}

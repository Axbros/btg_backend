package com.btg.commission.security;

import com.btg.commission.common.api.ApiResult;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.enums.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 资料未完善（-1）仅允许完善资料相关接口；待审核（0）仅允许读取本人基本信息；
 * 审核通过（1）禁止再次修改资料。
 */
@Component
@RequiredArgsConstructor
public class UserLifecycleAccessFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH = new AntPathMatcher();

    private final ObjectMapper objectMapper;

    private static boolean anyMatch(HttpServletRequest request, List<PathRule> rules) {
        String path = normalizePath(stripContextPath(request));
        String method = request.getMethod();
        for (PathRule r : rules) {
            if (r.method.equalsIgnoreCase(method) && PATH.match(r.pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizePath(String path) {
        if (path != null && path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (loginUser.isAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }
        UserStatus st = loginUser.getAccountStatus();
        if (st == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (st == UserStatus.NORMAL) {
            if ("PUT".equalsIgnoreCase(request.getMethod())
                    && PATH.match("/api/v1/user/profile", normalizePath(stripContextPath(request)))) {
                writeJson(response, ApiResult.fail(ResultCode.FORBIDDEN, "资料已审核通过，不可修改"));
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (st == UserStatus.PROFILE_INCOMPLETE) {
            List<PathRule> allowed = List.of(
                    new PathRule("GET", "/api/v1/me"),
                    new PathRule("GET", "/api/v1/user/me"),
                    new PathRule("GET", "/api/v1/user/profile"),
                    new PathRule("PUT", "/api/v1/user/profile"));
            if (anyMatch(request, allowed)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeJson(response, ApiResult.fail(ResultCode.FORBIDDEN, "请先完善并提交资料"));
            return;
        }

        if (st == UserStatus.PENDING_APPROVAL) {
            List<PathRule> allowed = List.of(
                    new PathRule("GET", "/api/v1/me"),
                    new PathRule("GET", "/api/v1/user/me"));
            if (anyMatch(request, allowed)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeJson(response, ApiResult.fail(ResultCode.FORBIDDEN, "资料审核中，暂不可操作"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String stripContextPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            return path.substring(ctx.length());
        }
        return path;
    }

    private void writeJson(HttpServletResponse response, ApiResult<?> body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private record PathRule(String method, String pattern) {
    }
}

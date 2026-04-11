package com.btg.commission.security;

import com.btg.commission.common.api.ResultCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginUserService loginUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            Long userId = jwtTokenProvider.getUserId(token);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                LoginUser user = loginUserService.loadByUserId(userId);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute("jwtError", ResultCode.UNAUTHORIZED.getCode());
            request.setAttribute("jwtMessage", "token expired");
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute("jwtError", ResultCode.UNAUTHORIZED.getCode());
            request.setAttribute("jwtMessage", "invalid token");
        }
        filterChain.doFilter(request, response);
    }
}

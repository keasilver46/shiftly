package com.shiftly.domain.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.shiftly.global.exception.BusinessException;

/**
 * Authorization: Bearer {accessToken} 을 읽어 SecurityContext 에 인증을 넣는다.
 * 토큰이 없거나 잘못되면 인증을 넣지 않고 넘긴다 → 보호된 경로면 EntryPoint 가 401 을 만든다.
 * 실패 원인(만료/위조)은 request attribute 로 넘겨 EntryPoint 가 에러 코드를 구분할 수 있게 한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_ERROR = "shiftly.authError";
    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = tokenProvider.parse(token);
                if (tokenProvider.isType(claims, JwtTokenProvider.TYPE_ACCESS)) {
                    UserPrincipal principal = tokenProvider.toPrincipal(claims);
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (BusinessException e) {
                request.setAttribute(ATTR_AUTH_ERROR, e.getErrorCode());
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            return header.substring(BEARER.length());
        }
        return null;
    }
}

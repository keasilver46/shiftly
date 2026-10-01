package com.shiftly.domain.auth.security;

import com.shiftly.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 401/403 을 ApiResponse 와 같은 JSON 으로 내려준다.
 * 필터 단계에서 발생해 @RestControllerAdvice 가 잡을 수 없으므로 직접 쓴다.
 */
public final class JsonAuthenticationHandlers {

    private JsonAuthenticationHandlers() {}

    public static AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> {
            Object attr = request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR);
            ErrorCode code = attr instanceof ErrorCode ec ? ec : ErrorCode.UNAUTHORIZED;
            write(response, code);
        };
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) ->
                write(response, ErrorCode.FORBIDDEN);
    }

    private static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"data":null,"error":{"code":"%s","message":"%s","details":null}}
                """.formatted(code.name(), code.getMessage()).trim());
    }
}

package com.shiftly.global.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러가 아직 없으므로 "인증/인가 단계를 통과했는가"만 본다.
 * 통과하면 404(핸들러 없음), 막히면 401/403.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("인증 경로는 토큰 없이 접근 가능하다 (보안 통과 후 검증 실패 400)")
    void authPathIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/auth/me 는 공개 경로가 아니다")
    void mePathRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("보호된 경로는 토큰 없으면 401 이다 (리다이렉트 아님)")
    void protectedPathWithoutAuthIs401() throws Exception {
        mockMvc.perform(get("/api/v1/attendances/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자 경로는 EMPLOYEE 권한으로 403 이다")
    void adminPathWithEmployeeRoleIs403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary").with(user("emp").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 경로는 ADMIN 권한으로 통과한다")
    void adminPathWithAdminRolePasses() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("허용 오리진의 CORS preflight 는 통과한다")
    void corsPreflightFromAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/attendances/today")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("허용되지 않은 오리진의 CORS preflight 는 거부된다")
    void corsPreflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/attendances/today")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}

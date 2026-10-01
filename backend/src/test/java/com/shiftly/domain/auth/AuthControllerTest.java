package com.shiftly.domain.auth;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shiftly.domain.auth.repository.RefreshTokenRepository;
import com.shiftly.domain.auth.service.AuthService;
import com.shiftly.domain.user.entity.Organization;
import com.shiftly.domain.user.entity.Role;
import com.shiftly.domain.user.entity.User;
import com.shiftly.domain.user.repository.OrganizationRepository;
import com.shiftly.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    static final String PASSWORD = "password1234";

    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User admin;
    User employee;

    @BeforeEach
    void setUp() {
        Organization org = organizationRepository.save(Organization.builder().name("매장 A").build());
        String hash = passwordEncoder.encode(PASSWORD);
        admin = userRepository.save(User.builder()
                .organization(org).email(AuthService.DEMO_ADMIN_EMAIL).name("관리자")
                .role(Role.ADMIN).passwordHash(hash).build());
        employee = userRepository.save(User.builder()
                .organization(org).email(AuthService.DEMO_EMPLOYEE_EMAIL).name("김직원")
                .role(Role.EMPLOYEE).passwordHash(hash).hourlyWage(10_030).build());
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("올바른 자격이면 access/refresh 토큰을 발급하고 refresh 는 DB 에 저장된다")
        void success() throws Exception {
            mockMvc.perform(loginRequest(employee.getEmail(), PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
                    .andExpect(jsonPath("$.error").doesNotExist());

            org.assertj.core.api.Assertions.assertThat(refreshTokenRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401 AUTH_INVALID_CREDENTIALS")
        void wrongPassword() throws Exception {
            mockMvc.perform(loginRequest(employee.getEmail(), "wrong"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("없는 이메일도 같은 401 코드 (계정 존재 여부 노출 방지)")
        void unknownEmail() throws Exception {
            mockMvc.perform(loginRequest("nobody@shiftly.demo", PASSWORD))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("이메일 형식이 아니면 400 VALIDATION_ERROR 와 필드 정보")
        void invalidEmailFormat() throws Exception {
            mockMvc.perform(loginRequest("not-an-email", PASSWORD))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.details[0].field").value("email"));
        }
    }

    @Nested
    @DisplayName("POST /auth/demo-login")
    class DemoLogin {

        @Test
        @DisplayName("ADMIN 역할로 요청하면 관리자 토큰이 나온다")
        void adminDemo() throws Exception {
            String access = extract(mockMvc.perform(post("/api/v1/auth/demo-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isOk())
                    .andReturn(), "$.data.accessToken");

            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.role").value("ADMIN"))
                    .andExpect(jsonPath("$.data.email").value(AuthService.DEMO_ADMIN_EMAIL));
        }
    }

    @Nested
    @DisplayName("GET /auth/me")
    class Me {

        @Test
        @DisplayName("access 토큰으로 내 정보와 조직을 조회한다")
        void withValidToken() throws Exception {
            String access = login(employee);
            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(employee.getId()))
                    .andExpect(jsonPath("$.data.name").value("김직원"))
                    .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                    .andExpect(jsonPath("$.data.organization.name").value("매장 A"));
        }

        @Test
        @DisplayName("토큰이 없으면 401 UNAUTHORIZED JSON")
        void withoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("위조된 토큰이면 401 AUTH_TOKEN_INVALID")
        void withTamperedToken() throws Exception {
            String access = login(employee);
            String tampered = access.substring(0, access.length() - 4) + "abcd";
            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tampered))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("refresh 토큰으로는 API 를 호출할 수 없다")
        void refreshTokenCannotAccessApi() throws Exception {
            String refresh = extract(mockMvc.perform(loginRequest(employee.getEmail(), PASSWORD)).andReturn(),
                    "$.data.refreshToken");
            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + refresh))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("유효한 refresh 로 새 토큰 쌍을 받고, 사용한 refresh 는 폐기된다 (회전)")
        void rotate() throws Exception {
            MvcResult loginResult = mockMvc.perform(loginRequest(employee.getEmail(), PASSWORD)).andReturn();
            String oldRefresh = extract(loginResult, "$.data.refreshToken");

            mockMvc.perform(refreshRequest(oldRefresh))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").value(not(oldRefresh)));

            // 같은 refresh 재사용 → 거부
            mockMvc.perform(refreshRequest(oldRefresh))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("access 토큰을 refresh 자리에 넣으면 거부된다")
        void accessTokenIsNotRefresh() throws Exception {
            String access = login(employee);
            mockMvc.perform(refreshRequest(access))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("로그아웃하면 refresh 가 삭제되어 재발급이 안 된다")
        void logoutInvalidatesRefresh() throws Exception {
            String refresh = extract(mockMvc.perform(loginRequest(employee.getEmail(), PASSWORD)).andReturn(),
                    "$.data.refreshToken");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refresh + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(refreshRequest(refresh))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── helpers ──────────────────────────────────────────────

    private org.springframework.test.web.servlet.RequestBuilder loginRequest(String email, String password) {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private org.springframework.test.web.servlet.RequestBuilder refreshRequest(String refreshToken) {
        return post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}");
    }

    private String login(User user) throws Exception {
        return extract(mockMvc.perform(loginRequest(user.getEmail(), PASSWORD)).andReturn(), "$.data.accessToken");
    }

    private static String extract(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}

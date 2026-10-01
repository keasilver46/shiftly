package com.shiftly.global.config;

import com.shiftly.domain.auth.service.AuthService;
import com.shiftly.domain.user.entity.Organization;
import com.shiftly.domain.user.entity.Role;
import com.shiftly.domain.user.entity.User;
import com.shiftly.domain.user.repository.OrganizationRepository;
import com.shiftly.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * local 프로파일에서만 실행. 비어 있을 때 테스트 조직/계정을 만든다.
 * 모든 계정 비밀번호: password1234
 */
@Slf4j
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer {

    public static final String DEMO_PASSWORD = "password1234";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner seedLocalData() {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }
            Organization org = organizationRepository.save(
                    Organization.builder().name("매장 A").timezone("Asia/Seoul").build());
            String hash = passwordEncoder.encode(DEMO_PASSWORD);

            userRepository.save(user(org, AuthService.DEMO_ADMIN_EMAIL, "관리자", Role.ADMIN, hash, null));
            userRepository.save(user(org, AuthService.DEMO_EMPLOYEE_EMAIL, "김직원", Role.EMPLOYEE, hash, 10_030));
            userRepository.saveAll(List.of(
                    user(org, "lee@shiftly.demo", "이직원", Role.EMPLOYEE, hash, 10_030),
                    user(org, "park@shiftly.demo", "박직원", Role.EMPLOYEE, hash, 10_500),
                    user(org, "choi@shiftly.demo", "최민수", Role.EMPLOYEE, hash, 11_000),
                    user(org, "jung@shiftly.demo", "정수진", Role.EMPLOYEE, hash, 10_030)
            ));
            log.info("[seed] 테스트 데이터 생성 완료. 관리자 {} / 직원 {} / 비밀번호 {}",
                    AuthService.DEMO_ADMIN_EMAIL, AuthService.DEMO_EMPLOYEE_EMAIL, DEMO_PASSWORD);
        };
    }

    private User user(Organization org, String email, String name, Role role, String hash, Integer wage) {
        return User.builder()
                .organization(org).email(email).name(name).role(role).passwordHash(hash).hourlyWage(wage)
                .build();
    }
}

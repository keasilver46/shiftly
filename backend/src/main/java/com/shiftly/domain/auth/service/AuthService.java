package com.shiftly.domain.auth.service;

import com.shiftly.domain.auth.dto.MeResponse;
import com.shiftly.domain.auth.dto.TokenResponse;
import com.shiftly.domain.auth.entity.RefreshToken;
import com.shiftly.domain.auth.repository.RefreshTokenRepository;
import com.shiftly.domain.auth.security.JwtTokenProvider;
import com.shiftly.domain.user.entity.Role;
import com.shiftly.domain.user.entity.User;
import com.shiftly.domain.user.repository.UserRepository;
import com.shiftly.global.exception.BusinessException;
import com.shiftly.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    /** 포트폴리오 체험용 계정. local 프로파일의 초기 데이터에서 생성된다. */
    public static final String DEMO_ADMIN_EMAIL = "admin@shiftly.demo";
    public static final String DEMO_EMPLOYEE_EMAIL = "employee@shiftly.demo";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public TokenResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            // 이메일 존재 여부를 노출하지 않도록 같은 에러 코드를 쓴다
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    public TokenResponse demoLogin(Role role) {
        String email = role == Role.ADMIN ? DEMO_ADMIN_EMAIL : DEMO_EMPLOYEE_EMAIL;
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEMO_ACCOUNT_NOT_FOUND));
        return issueTokens(user);
    }

    /**
     * 리프레시 토큰 회전: 받은 토큰은 폐기하고 새 쌍을 발급한다.
     * DB 에 없는 토큰(이미 사용됨/로그아웃됨)은 거부 → 탈취된 토큰 재사용 방지.
     */
    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseRefreshClaims(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        refreshTokenRepository.delete(stored);
        if (stored.isExpired()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        User user = userRepository.findById(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = userRepository.findWithOrganizationById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return MeResponse.from(user);
    }

    private TokenResponse issueTokens(User user) {
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        String access = tokenProvider.createAccessToken(user);
        String refresh = tokenProvider.createRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(refresh)
                .expiresAt(tokenProvider.refreshTokenExpiresAt())
                .build());
        return new TokenResponse(access, refresh, tokenProvider.accessTokenExpiry());
    }

    private Claims parseRefreshClaims(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        if (!tokenProvider.isType(claims, JwtTokenProvider.TYPE_REFRESH)) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        return claims;
    }
}

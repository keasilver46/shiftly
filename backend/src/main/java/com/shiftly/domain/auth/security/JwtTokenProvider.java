package com.shiftly.domain.auth.security;

import com.shiftly.domain.user.entity.Role;
import com.shiftly.domain.user.entity.User;
import com.shiftly.global.config.JwtProperties;
import com.shiftly.global.exception.BusinessException;
import com.shiftly.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** JWT 생성/검증. access 와 refresh 는 type 클레임으로 구분한다. */
@Component
public class JwtTokenProvider {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        return build(user, TYPE_ACCESS, properties.accessTokenExpiry());
    }

    public String createRefreshToken(User user) {
        return build(user, TYPE_REFRESH, properties.refreshTokenExpiry());
    }

    public long accessTokenExpiry() {
        return properties.accessTokenExpiry();
    }

    public Instant refreshTokenExpiresAt() {
        return Instant.now().plusSeconds(properties.refreshTokenExpiry());
    }

    /** 서명/만료 검증 후 클레임 반환. 실패 시 ErrorCode 를 담은 BusinessException. */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    public UserPrincipal toPrincipal(Claims claims) {
        return new UserPrincipal(Long.valueOf(claims.getSubject()), Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    public boolean isType(Claims claims, String type) {
        return type.equals(claims.get(CLAIM_TYPE, String.class));
    }

    private String build(User user, String type, long expirySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .id(UUID.randomUUID().toString())   // 같은 초에 발급돼도 토큰 문자열이 달라지도록
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }
}

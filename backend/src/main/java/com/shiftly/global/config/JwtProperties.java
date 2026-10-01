package com.shiftly.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jwt.* 설정. 만료 시간 단위는 초.
 * secret 은 HS256 기준 32바이트 이상이어야 한다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long accessTokenExpiry, long refreshTokenExpiry) {}

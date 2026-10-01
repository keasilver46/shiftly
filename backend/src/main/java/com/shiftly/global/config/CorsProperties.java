package com.shiftly.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 오리진 설정. application-{profile}.yml 의 app.cors.allowed-origins 에서 읽는다.
 * 로컬은 Vite 개발 서버(5173), 운영은 실제 프론트 도메인.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173");
        }
    }
}

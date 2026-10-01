package com.shiftly.domain.auth.controller;

import com.shiftly.domain.auth.dto.DemoLoginRequest;
import com.shiftly.domain.auth.dto.LoginRequest;
import com.shiftly.domain.auth.dto.MeResponse;
import com.shiftly.domain.auth.dto.RefreshRequest;
import com.shiftly.domain.auth.dto.TokenResponse;
import com.shiftly.domain.auth.security.UserPrincipal;
import com.shiftly.domain.auth.service.AuthService;
import com.shiftly.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.email(), request.password()));
    }

    @PostMapping("/demo-login")
    public ApiResponse<TokenResponse> demoLogin(@Valid @RequestBody DemoLoginRequest request) {
        return ApiResponse.ok(authService.demoLogin(request.role()));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok();
    }

    /** 이 경로만 인증 필요 (SecurityConfig 의 공개 경로에서 제외) */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(authService.me(principal.id()));
    }
}

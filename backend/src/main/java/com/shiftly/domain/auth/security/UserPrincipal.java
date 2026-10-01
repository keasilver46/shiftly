package com.shiftly.domain.auth.security;

import com.shiftly.domain.user.entity.Role;

/** SecurityContext 에 들어가는 인증 주체. DB 를 다시 조회하지 않도록 토큰에 있는 정보만 담는다. */
public record UserPrincipal(Long id, Role role) {}

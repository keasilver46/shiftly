package com.shiftly.domain.auth.dto;

import com.shiftly.domain.user.entity.Role;
import com.shiftly.domain.user.entity.User;

public record MeResponse(Long id, String email, String name, Role role, OrganizationSummary organization) {

    public record OrganizationSummary(Long id, String name) {}

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                new OrganizationSummary(user.getOrganization().getId(), user.getOrganization().getName())
        );
    }
}

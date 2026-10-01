package com.shiftly.domain.auth.dto;

import com.shiftly.domain.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record DemoLoginRequest(@NotNull Role role) {}

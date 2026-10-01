package com.shiftly.domain.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {}

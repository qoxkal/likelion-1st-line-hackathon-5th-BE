package com.line1.booktalk.domain.auth.dto;

/**
 * POST /auth/guest 응답.
 * 해커톤 단순화로 refreshToken은 주지 않는다 (명세서와 다른 부분).
 */
public record GuestLoginResponse(
        UserSummary user,
        String accessToken,
        long expiresIn
) {
    public record UserSummary(String id, String type) {
    }
}

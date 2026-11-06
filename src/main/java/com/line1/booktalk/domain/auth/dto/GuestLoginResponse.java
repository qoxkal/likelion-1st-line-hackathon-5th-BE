package com.line1.booktalk.domain.auth.dto;

/**
 * POST /auth/guest 응답.
 * 게스트 로그인 시 access/refresh token 쌍을 발급한다.
 */
public record GuestLoginResponse(
        UserSummary user,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    public record UserSummary(String id, String type) {
    }
}

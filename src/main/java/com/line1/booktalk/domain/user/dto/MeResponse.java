package com.line1.booktalk.domain.user.dto;

import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.global.common.Language;

import java.time.Instant;

/** GET /me, PATCH /me 응답 */
public record MeResponse(
        String id,
        String type,
        String displayName,
        Language preferredLanguage,
        boolean notificationEnabled,
        Instant createdAt
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getType().name(),
                user.getDisplayName(),
                user.getPreferredLanguage(),
                user.isNotificationEnabled(),
                user.getCreatedAt()
        );
    }
}

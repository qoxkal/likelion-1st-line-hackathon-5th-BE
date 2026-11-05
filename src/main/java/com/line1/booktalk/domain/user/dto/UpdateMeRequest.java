package com.line1.booktalk.domain.user.dto;

import com.line1.booktalk.global.common.Language;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH /me 요청. 보낸 필드만 바꾸고, 안 보낸 필드(null)는 그대로 둔다.
 */
public record UpdateMeRequest(
        @Size(min = 1, max = 30, message = "이름은 1~30자여야 해요.")
        @Pattern(regexp = ".*\\S.*", message = "이름을 입력해 주세요.")
        String displayName,

        Language preferredLanguage,

        Boolean notificationEnabled
) {
}

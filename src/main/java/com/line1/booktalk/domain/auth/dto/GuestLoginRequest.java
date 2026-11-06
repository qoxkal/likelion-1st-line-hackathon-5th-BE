package com.line1.booktalk.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /auth/guest 요청 */
public record GuestLoginRequest(
        @NotBlank(message = "deviceId는 필수예요.")
        @Size(max = 100, message = "deviceId는 100자 이하여야 해요.")
        String deviceId,

        @NotBlank(message = "locale은 필수예요.")
        @Size(max = 10, message = "locale은 10자 이하여야 해요.")
        String locale
) {
}

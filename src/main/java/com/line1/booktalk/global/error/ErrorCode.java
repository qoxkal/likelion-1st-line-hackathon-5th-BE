package com.line1.booktalk.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 명세서 2.4의 오류 코드 목록.
 * status: HTTP 상태 코드, retryable: 클라이언트가 다시 시도해도 되는지
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    VALIDATION_ERROR(400, "요청 값을 확인해 주세요.", false),
    INVALID_STATE(400, "지금은 처리할 수 없는 요청이에요.", false),

    // 401
    UNAUTHORIZED(401, "인증이 필요해요.", false),
    TOKEN_EXPIRED(401, "인증이 만료됐어요. 앱을 다시 시작해 주세요.", false),

    // 403
    FORBIDDEN(403, "권한이 없어요.", false),

    // 404
    RESOURCE_NOT_FOUND(404, "요청한 정보를 찾을 수 없어요.", false),

    // 405
    METHOD_NOT_ALLOWED(405, "지원하지 않는 요청 방식이에요.", false),

    // 409
    DUPLICATE_RESOURCE(409, "이미 존재하는 정보예요.", false),
    CHILD_LIMIT_EXCEEDED(409, "아이 프로필을 더 만들 수 없어요.", false),
    ANALYSIS_ALREADY_RUNNING(409, "이 페이지는 이미 분석 중이에요.", false),

    // 413, 415
    FILE_TOO_LARGE(413, "15MB 이하 사진만 올릴 수 있어요.", false),
    UNSUPPORTED_MEDIA_TYPE(415, "JPG, PNG, WEBP 사진만 올릴 수 있어요.", false),

    // 422
    IMAGE_QUALITY_LOW(422, "글자가 잘 보이지 않아요. 다시 찍어 주세요.", false),
    CONTENT_UNSAFE(422, "처리할 수 없는 내용이 포함돼 있어요.", false),

    // 429
    RATE_LIMITED(429, "요청이 너무 많아요. 잠시 후 다시 시도해 주세요.", true),
    AI_QUOTA_EXCEEDED(429, "오늘 사용할 수 있는 횟수를 모두 썼어요.", false),

    // 500, 503
    INTERNAL_ERROR(500, "잠시 후 다시 시도해 주세요.", true),
    AI_PROVIDER_UNAVAILABLE(503, "AI 서비스가 잠시 응답하지 않아요. 잠시 후 다시 시도해 주세요.", true);

    private final int status;
    private final String message;
    private final boolean retryable;
}

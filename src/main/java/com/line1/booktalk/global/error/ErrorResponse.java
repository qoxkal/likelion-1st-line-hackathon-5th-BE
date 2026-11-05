package com.line1.booktalk.global.error;

import com.line1.booktalk.global.response.Meta;

import java.util.List;

/**
 * 실패 응답 공통 형식 (명세서 2.4).
 * { "error": { "code", "message", "details", "retryable" }, "meta": { ... } }
 */
public record ErrorResponse(ErrorBody error, Meta meta) {

    public record ErrorBody(String code, String message, List<FieldDetail> details, boolean retryable) {
    }

    public record FieldDetail(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode code) {
        return of(code, code.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode code, String message, List<FieldDetail> details) {
        return new ErrorResponse(
                new ErrorBody(code.name(), message, details, code.isRetryable()),
                Meta.now()
        );
    }
}

package com.line1.booktalk.global.response;

/**
 * 성공 응답 공통 형식 (명세서 2.3 응답 Envelope).
 * { "data": ..., "meta": { "requestId": ..., "timestamp": ... } }
 */
public record ApiResponse<T>(T data, Meta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Meta.now());
    }

    /** cursor 페이지네이션 목록 응답 (명세서 2.5) */
    public static <T> ApiResponse<T> page(T data, String nextCursor, boolean hasNext) {
        return new ApiResponse<>(data, Meta.page(nextCursor, hasNext));
    }
}

package com.line1.booktalk.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.line1.booktalk.global.filter.RequestIdFilter;

import java.time.Instant;

/**
 * 모든 응답에 붙는 meta 정보.
 * 목록 API에서만 nextCursor, hasNext를 채운다. 값이 null이면 응답에서 빠진다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Meta(
        String requestId,
        Instant timestamp,
        String nextCursor,
        Boolean hasNext
) {
    public static Meta now() {
        return new Meta(RequestIdFilter.currentRequestId(), Instant.now(), null, null);
    }

    public static Meta page(String nextCursor, boolean hasNext) {
        return new Meta(RequestIdFilter.currentRequestId(), Instant.now(), nextCursor, hasNext);
    }
}

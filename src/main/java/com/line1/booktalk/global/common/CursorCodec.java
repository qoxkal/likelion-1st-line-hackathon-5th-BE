package com.line1.booktalk.global.common;

import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * cursor 페이지네이션용 커서 (명세서 2.5).
 * "마지막으로 본 항목의 createdAt|id"를 Base64로 감싼 문자열이다.
 * 다음 페이지는 이 항목보다 뒤(더 오래된) 것부터 가져온다.
 */
public final class CursorCodec {

    private CursorCodec() {
    }

    public record Cursor(Instant createdAt, String id) {
    }

    public static String encode(Instant createdAt, String id) {
        String raw = createdAt.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            return new Cursor(Instant.parse(raw.substring(0, sep)), raw.substring(sep + 1));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "cursor 값이 올바르지 않아요.");
        }
    }
}

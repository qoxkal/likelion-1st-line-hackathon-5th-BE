package com.line1.booktalk.domain.book;

/** 도서 출처 (명세서 GET /books의 source) */
public enum BookSource {
    DEMO,           // 시연용으로 직접 만든 이야기
    MY_LIBRARY,     // 사용자가 촬영해서 등록한 책
    RECOMMENDED     // 추천 도서
}

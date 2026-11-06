package com.line1.booktalk.domain.book;

/** 촬영 페이지 상태 */
public enum PageStatus {
    UPLOADED,   // 사진 업로드 완료, 분석 전
    ANALYZING,  // 분석 중
    ANALYZED,   // 분석 완료 (원본 사진은 삭제됨)
    FAILED      // 분석 실패 (재시도용으로 원본 보관)
}

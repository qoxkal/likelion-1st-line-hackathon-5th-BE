package com.line1.booktalk.domain.analysis.dto;

/** 분석 시작 응답. 클라이언트는 pollAfterMs 뒤에 GET /analysis/{jobId}로 상태를 확인한다 */
public record AnalyzePageResponse(String jobId, String status, long pollAfterMs) {
}

package com.line1.booktalk.domain.analysis;

/** 분석 작업이 DB에 저장(커밋)된 뒤 작업자를 깨우기 위한 이벤트 */
public record AnalysisRequestedEvent(String jobId) {
}

package com.line1.booktalk.domain.analysis;

/**
 * RUNNING 상태 안에서 지금 어떤 단계인지 (화면 진행 표시용).
 * 실제로는 AI를 한 번만 호출하지만, 사용자에게는 단계를 나눠 보여준다.
 */
public enum AnalysisStage {
    READING_TEXT,   // 글자 읽는 중
    EXTRACTING,     // 표현 찾는 중
    TRANSLATING,    // 번역 중
    SAVING          // 결과 저장 중
}

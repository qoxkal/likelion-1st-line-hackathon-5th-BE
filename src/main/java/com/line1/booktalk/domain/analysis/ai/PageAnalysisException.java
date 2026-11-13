package com.line1.booktalk.domain.analysis.ai;

import lombok.Getter;

/**
 * 페이지 분석이 실패한 이유를 작업(job)에 기록하기 위한 예외.
 * - code: 작업에 저장할 오류 코드 (프론트가 이 값으로 안내 문구를 고를 수 있다)
 * - userMessage: 사용자에게 보여줄 문구
 * - rejected: true면 FAILED가 아니라 REJECTED (사진 품질 문제처럼 다시 찍어야 하는 경우)
 * - retryable: 같은 사진으로 다시 시도해도 되는지
 */
@Getter
public class PageAnalysisException extends RuntimeException {

    private final String code;
    private final String userMessage;
    private final boolean rejected;
    private final boolean retryable;

    public PageAnalysisException(String code, String userMessage, boolean rejected, boolean retryable) {
        super(code);
        this.code = code;
        this.userMessage = userMessage;
        this.rejected = rejected;
        this.retryable = retryable;
    }
}

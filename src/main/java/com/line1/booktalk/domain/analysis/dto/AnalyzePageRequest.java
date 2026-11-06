package com.line1.booktalk.domain.analysis.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /books/{bookId}/pages/analyze 요청
 * - pageId: 분석할 페이지 (필수)
 * - childId: 연령과 설명 언어 기준이 될 아이. 없으면 페이지를 올린 아이 기준
 */
public record AnalyzePageRequest(
        @NotBlank(message = "pageId는 필수예요.")
        String pageId,

        String childId
) {
}

package com.line1.booktalk.domain.book.dto;

import com.line1.booktalk.domain.book.Book;

import java.util.List;

/** GET /books 목록의 한 항목 */
public record BookSummaryResponse(
        String id,
        String title,
        String author,
        String source,
        List<Integer> targetAges,
        int expressionCount,
        String coverUrl
) {
    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getSource().name(),
                book.getTargetAges(),
                book.getExpressionCount(),
                book.getCoverUrl()
        );
    }
}

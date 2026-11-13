package com.line1.booktalk.domain.book.dto;

import com.line1.booktalk.domain.book.Book;
import com.line1.booktalk.domain.analysis.PageExpression;

import java.util.List;

/** GET /books 목록의 한 항목 */
public record BookSummaryResponse(
        String id,
        String title,
        String author,
        String source,
        List<Integer> targetAges,
        int expressionCount,
        String coverUrl,
        List<VocabularySummary> expressions
) {
    public record VocabularySummary(String id, String text) {
        public static VocabularySummary from(PageExpression expression) {
            return new VocabularySummary(expression.getId(), expression.getText());
        }
    }

    public static BookSummaryResponse from(Book book, List<PageExpression> expressions) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getSource().name(),
                book.getTargetAges(),
                book.getExpressionCount(),
                book.getCoverUrl(),
                expressions.stream().map(VocabularySummary::from).toList()
        );
    }
}

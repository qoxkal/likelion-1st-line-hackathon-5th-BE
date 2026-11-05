package com.line1.booktalk.domain.book.dto;

import com.line1.booktalk.domain.book.Book;
import com.line1.booktalk.domain.book.BookPage;

import java.util.List;

/** GET /books/{bookId} 응답 */
public record BookDetailResponse(
        String id,
        String title,
        String author,
        String source,
        List<Integer> targetAges,
        int expressionCount,
        String coverUrl,
        Progress progress,
        List<PageSummary> pages
) {
    public record Progress(int exploredWords, int totalWords) {
    }

    public record PageSummary(String id, String pageLabel, String analysisStatus) {
        public static PageSummary from(BookPage page) {
            return new PageSummary(page.getId(), page.getPageLabel(), toAnalysisStatus(page));
        }

        /** 페이지 상태를 명세서의 analysisStatus 값으로 바꾼다 */
        private static String toAnalysisStatus(BookPage page) {
            return switch (page.getStatus()) {
                case UPLOADED -> "NOT_STARTED";
                case ANALYZING -> "RUNNING";
                case ANALYZED -> "COMPLETED";
                case FAILED -> "FAILED";
            };
        }
    }

    public static BookDetailResponse of(Book book, int exploredWords, List<BookPage> pages) {
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getSource().name(),
                book.getTargetAges(),
                book.getExpressionCount(),
                book.getCoverUrl(),
                new Progress(exploredWords, book.getExpressionCount()),
                pages.stream().map(PageSummary::from).toList()
        );
    }
}

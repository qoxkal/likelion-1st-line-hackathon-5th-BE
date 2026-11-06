package com.line1.booktalk.domain.book.dto;

import com.line1.booktalk.domain.book.BookPage;

/** POST /books/uploads 응답 */
public record UploadPageResponse(
        String pageId,
        String bookId,
        String pageLabel,
        String status
) {
    public static UploadPageResponse from(BookPage page) {
        return new UploadPageResponse(
                page.getId(),
                page.getBook().getId(),
                page.getPageLabel(),
                page.getStatus().name()
        );
    }
}

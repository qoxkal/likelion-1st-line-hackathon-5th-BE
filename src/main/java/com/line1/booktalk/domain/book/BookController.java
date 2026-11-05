package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.book.dto.BookDetailResponse;
import com.line1.booktalk.domain.book.dto.BookSummaryResponse;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * 도서 목록
     * 예: GET /books?source=DEMO&age=4&limit=20
     */
    @GetMapping
    public ApiResponse<List<BookSummaryResponse>> getBooks(
            @LoginUser String userId,
            @RequestParam(required = false) BookSource source,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        BookService.BookPageResult result = bookService.getBooks(userId, source, age, query, cursor, limit);
        return ApiResponse.page(result.books(), result.nextCursor(), result.hasNext());
    }

    /** 도서 상세: 메타데이터, 진행률, 내가 찍은 페이지 목록 */
    @GetMapping("/{bookId}")
    public ApiResponse<BookDetailResponse> getBook(@LoginUser String userId,
                                                   @PathVariable String bookId) {
        return ApiResponse.of(bookService.getBook(userId, bookId));
    }
}

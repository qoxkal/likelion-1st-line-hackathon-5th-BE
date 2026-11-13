package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.book.dto.BookDetailResponse;
import com.line1.booktalk.domain.book.dto.BookSummaryResponse;
import com.line1.booktalk.domain.book.dto.UploadPageResponse;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookUploadService bookUploadService;

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

    /** 내 책장 도서 이름 바꾸기: PATCH /books/{bookId} { "title": "새 이름" } */
    @PatchMapping("/{bookId}")
    public ApiResponse<BookDetailResponse> rename(@LoginUser String userId, @PathVariable String bookId,
                                                  @Valid @RequestBody RenameRequest request) {
        return ApiResponse.of(bookService.rename(userId, bookId, request.title()));
    }

    public record RenameRequest(@NotBlank(message = "책 이름을 입력해 주세요.")
                                @Size(max = 100, message = "책 이름은 100자 이하여야 해요.") String title) {
    }

    /**
     * 그림책 페이지 사진 업로드 + 페이지 등록 (multipart/form-data)
     * - file: 사진 (jpeg, png, webp / 최대 4MB)
     * - childId: 학습 대상 아이 (필수)
     * - bookId: 기존 도서에 추가할 때
     * - bookTitle: bookId가 없을 때 새 도서 제목 (내 책장에 만들어짐)
     * - pageLabel: 예) 4-5쪽
     */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadPageResponse> upload(
            @LoginUser String userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String childId,
            @RequestParam(required = false) String bookId,
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) String pageLabel) {
        return ApiResponse.of(bookUploadService.upload(userId, childId, bookId, bookTitle, pageLabel, file));
    }
}

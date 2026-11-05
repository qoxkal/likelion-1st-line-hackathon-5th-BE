package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.book.dto.BookDetailResponse;
import com.line1.booktalk.domain.book.dto.BookSummaryResponse;
import com.line1.booktalk.global.common.CursorCodec;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private static final int MAX_LIMIT = 100;

    private final BookRepository bookRepository;
    private final BookPageRepository bookPageRepository;

    public record BookPageResult(List<BookSummaryResponse> books, String nextCursor, boolean hasNext) {
    }

    /**
     * 도서 목록. 시연·추천 도서는 모두에게, 내 책장 도서는 주인에게만 보인다.
     * 최신순으로 정렬하고 cursor로 다음 페이지를 가져온다.
     */
    public BookPageResult getBooks(String userId, BookSource source, Integer age,
                                   String query, String cursor, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "limit은 1~100 사이여야 해요.");
        }
        CursorCodec.Cursor after = (cursor == null || cursor.isBlank()) ? null : CursorCodec.decode(cursor);

        Specification<Book> spec = searchSpec(userId, source, age, query, after);
        // 다음 페이지가 있는지 알기 위해 limit보다 1개 더 가져온다
        PageRequest page = PageRequest.of(0, limit + 1,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        List<Book> found = bookRepository.findAll(spec, page).getContent();

        boolean hasNext = found.size() > limit;
        List<Book> books = hasNext ? found.subList(0, limit) : found;
        String nextCursor = null;
        if (hasNext) {
            Book last = books.get(books.size() - 1);
            nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
        }

        return new BookPageResult(
                books.stream().map(BookSummaryResponse::from).toList(),
                nextCursor,
                hasNext
        );
    }

    public BookDetailResponse getBook(String userId, String bookId) {
        Book book = getVisibleBook(userId, bookId);
        List<BookPage> pages = bookPageRepository.findAllByBookIdAndOwnerIdOrderByCreatedAtAsc(bookId, userId);
        // TODO: 본 단어 수는 학습 기록(learning-events, 근영 담당)이 생기면 연결한다. 지금은 0
        int exploredWords = 0;
        return BookDetailResponse.of(book, exploredWords, pages);
    }

    /** 볼 수 없는 도서(남의 책장 도서)도 없는 도서처럼 404로 처리한다 */
    public Book getVisibleBook(String userId, String bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!book.isVisibleTo(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return book;
    }

    /** 넘어온 조건만 WHERE에 붙이는 동적 검색 조건 */
    private Specification<Book> searchSpec(String userId, BookSource source, Integer age,
                                           String query, CursorCodec.Cursor after) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1) 볼 수 있는 도서: 내 책장 도서가 아니거나, 내 책장 도서면 주인이 나
            //    owner가 없는 시연 도서도 포함해야 해서 LEFT JOIN을 쓴다
            Join<Object, Object> owner = root.join("owner", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.notEqual(root.get("source"), BookSource.MY_LIBRARY),
                    cb.equal(owner.get("id"), userId)
            ));

            // 2) 출처
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }

            // 3) 연령: minAge <= age <= maxAge
            if (age != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minAge"), age));
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxAge"), age));
            }

            // 4) 제목 또는 저자 검색
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like)
                ));
            }

            // 5) 커서: 마지막으로 본 항목보다 오래된 것만
            if (after != null) {
                predicates.add(cb.or(
                        cb.lessThan(root.get("createdAt"), after.createdAt()),
                        cb.and(
                                cb.equal(root.get("createdAt"), after.createdAt()),
                                cb.lessThan(root.get("id"), after.id())
                        )
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

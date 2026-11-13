package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.domain.book.BookSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageExpressionRepository extends JpaRepository<PageExpression, String> {

    List<PageExpression> findAllByPageIdOrderByCreatedAtAsc(String pageId);

    long countByPageBookIdAndPageOwnerId(String bookId, String ownerId);

    List<PageExpression> findAllByPageOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<PageExpression> findAllByPageBookIdOrderByCreatedAtAsc(String bookId);

    /** 시연용(DEMO) 도서처럼 출처별로 표현을 모두 가져온다 */
    List<PageExpression> findAllByPageBookSource(BookSource source);
}

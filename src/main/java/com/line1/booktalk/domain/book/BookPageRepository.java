package com.line1.booktalk.domain.book;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookPageRepository extends JpaRepository<BookPage, String> {

    /** 소유권 검사용 */
    Optional<BookPage> findByIdAndOwnerId(String id, String ownerId);

    /** 도서 상세에서 내가 찍은 페이지 목록 */
    List<BookPage> findAllByBookIdAndOwnerIdOrderByCreatedAtAsc(String bookId, String ownerId);
}

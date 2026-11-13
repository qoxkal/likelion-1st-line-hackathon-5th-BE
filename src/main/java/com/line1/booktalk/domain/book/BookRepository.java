package com.line1.booktalk.domain.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** 목록 검색은 조건이 선택적이라 Specification으로 동적으로 만든다 (BookService 참고) */
public interface BookRepository extends JpaRepository<Book, String>, JpaSpecificationExecutor<Book> {
    Optional<Book> findBySourceAndTitle(BookSource source, String title);
}

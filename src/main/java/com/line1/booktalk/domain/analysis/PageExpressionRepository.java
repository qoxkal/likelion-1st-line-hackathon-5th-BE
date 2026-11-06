package com.line1.booktalk.domain.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageExpressionRepository extends JpaRepository<PageExpression, String> {

    List<PageExpression> findAllByPageIdOrderByCreatedAtAsc(String pageId);

    long countByPageBookIdAndPageOwnerId(String bookId, String ownerId);

    List<PageExpression> findAllByPageOwnerIdOrderByCreatedAtDesc(String ownerId);
}

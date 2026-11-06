package com.line1.booktalk.domain.child;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChildRepository extends JpaRepository<Child, String> {

    List<Child> findAllByUserIdOrderByCreatedAtAsc(String userId);

    /** 소유권 검사용: 내 아이가 아니면 비어 있는 결과가 나온다 */
    Optional<Child> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}

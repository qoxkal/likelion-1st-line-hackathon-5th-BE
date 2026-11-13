package com.line1.booktalk.domain.child;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChildRepository extends JpaRepository<Child, String> {

    List<Child> findAllByUserIdOrderByCreatedAtAsc(String userId);

    /** 소유권 검사용: 내 아이가 아니면 비어 있는 결과가 나온다 */
    Optional<Child> findByIdAndUserId(String id, String userId);

    /** 대화 시작 동시 요청에서 ACTIVE 세션이 두 개 생기지 않도록 아이 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Child c where c.id = :id and c.user.id = :userId")
    Optional<Child> findOwnedByIdForUpdate(@Param("id") String id, @Param("userId") String userId);

    long countByUserId(String userId);
}

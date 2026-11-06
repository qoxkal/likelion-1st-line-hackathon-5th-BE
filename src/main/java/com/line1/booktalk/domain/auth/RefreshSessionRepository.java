package com.line1.booktalk.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    List<RefreshSession> findAllByUserIdAndRevokedFalse(String userId);
}

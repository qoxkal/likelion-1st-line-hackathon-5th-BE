package com.line1.booktalk.domain.auth;

import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "refresh_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshSession extends BaseEntity {
    @Id @Column(length = 40)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean revoked;

    public static RefreshSession create(User user, String tokenHash, Instant expiresAt) {
        RefreshSession session = new RefreshSession();
        session.id = IdGenerator.generate(IdGenerator.REFRESH_SESSION);
        session.user = user;
        session.tokenHash = tokenHash;
        session.expiresAt = expiresAt;
        return session;
    }

    public void revoke() { this.revoked = true; }
    public boolean isUsable() { return !revoked && expiresAt.isAfter(Instant.now()); }
}

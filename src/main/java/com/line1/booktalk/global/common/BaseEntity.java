package com.line1.booktalk.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 모든 엔티티의 공통 부모.
 * - createdAt, updatedAt 자동 기록
 * - id를 코드에서 직접 만들기 때문에(usr_..., chd_...), save() 할 때 새 데이터인지
 *   createdAt이 비어 있는지로 판단한다. (안 하면 저장 전에 불필요한 SELECT가 한 번 더 나간다)
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Persistable<String> {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}

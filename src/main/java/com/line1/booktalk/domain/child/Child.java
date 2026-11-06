package com.line1.booktalk.domain.child;

import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.IdGenerator;
import com.line1.booktalk.global.common.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이 프로필. 실명 대신 별칭(nickname)을 쓴다 (명세서 14.1).
 */
@Getter
@Entity
@Table(name = "children")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Child extends BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Language explanationLanguage;

    public static Child create(User user, String nickname, int age,
                               Relationship relationship, Language explanationLanguage) {
        Child child = new Child();
        child.id = IdGenerator.generate(IdGenerator.CHILD);
        child.user = user;
        child.nickname = nickname;
        child.age = age;
        child.relationship = relationship;
        child.explanationLanguage = explanationLanguage;
        return child;
    }

    /** PATCH /children/{childId}: null이 아닌 값만 바꾼다 */
    public void update(String nickname, Integer age,
                       Relationship relationship, Language explanationLanguage) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (age != null) {
            this.age = age;
        }
        if (relationship != null) {
            this.relationship = relationship;
        }
        if (explanationLanguage != null) {
            this.explanationLanguage = explanationLanguage;
        }
    }

    public boolean isOwnedBy(String userId) {
        return user.getId().equals(userId);
    }
}

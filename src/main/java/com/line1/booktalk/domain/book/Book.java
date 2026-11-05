package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.IdGenerator;
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

import java.util.List;
import java.util.stream.IntStream;

/**
 * 도서. 시연용 도서(DEMO)는 owner가 없고, 사용자가 등록한 책(MY_LIBRARY)은 owner가 있다.
 */
@Getter
@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookSource source;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String author;

    /** 사용자 책장 도서일 때만 값이 있다 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    /** 대상 연령. targetAges [4, 5]는 minAge 4, maxAge 5로 저장한다 */
    private Integer minAge;

    private Integer maxAge;

    @Column(length = 500)
    private String coverUrl;

    @Column(nullable = false)
    private int expressionCount;

    public static Book createDemo(String title, String author, int minAge, int maxAge, String coverUrl) {
        Book book = new Book();
        book.id = IdGenerator.generate(IdGenerator.BOOK);
        book.source = BookSource.DEMO;
        book.title = title;
        book.author = author;
        book.minAge = minAge;
        book.maxAge = maxAge;
        book.coverUrl = coverUrl;
        book.expressionCount = 0;
        return book;
    }

    public static Book createForUser(User owner, String title) {
        Book book = new Book();
        book.id = IdGenerator.generate(IdGenerator.BOOK);
        book.source = BookSource.MY_LIBRARY;
        book.title = title;
        book.owner = owner;
        book.expressionCount = 0;
        return book;
    }

    /** 응답용: [minAge, ..., maxAge] */
    public List<Integer> getTargetAges() {
        if (minAge == null || maxAge == null) {
            return List.of();
        }
        return IntStream.rangeClosed(minAge, maxAge).boxed().toList();
    }

    public void changeExpressionCount(int expressionCount) {
        this.expressionCount = expressionCount;
    }

    /** 시연용 도서는 누구나, 사용자 도서는 주인만 볼 수 있다 */
    public boolean isVisibleTo(String userId) {
        return source != BookSource.MY_LIBRARY || (owner != null && owner.getId().equals(userId));
    }
}

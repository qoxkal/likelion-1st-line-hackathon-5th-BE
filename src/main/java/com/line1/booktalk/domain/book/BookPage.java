package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.child.Child;
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

import java.time.Instant;

/**
 * 사용자가 촬영한 그림책 페이지.
 * 원본 사진은 분석이 끝나면 바로 지우고 storageKey를 비운다 (해커톤 단순화 5번).
 */
@Getter
@Entity
@Table(name = "book_pages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookPage extends BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(length = 20)
    private String pageLabel;

    /** 저장소 안의 파일 위치. 원본을 지우면 null */
    @Column(length = 300)
    private String storageKey;

    @Column(nullable = false, length = 30)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PageStatus status;

    private Instant originalDeletedAt;

    /** AI가 사진에서 읽은 글자 */
    @Column(length = 4000)
    private String ocrText;

    public static BookPage create(Book book, User owner, Child child, String pageLabel,
                                  String storageKey, String contentType, long sizeBytes, String sha256) {
        BookPage page = new BookPage();
        page.id = IdGenerator.generate(IdGenerator.BOOK_PAGE);
        page.book = book;
        page.owner = owner;
        page.child = child;
        page.pageLabel = pageLabel;
        page.storageKey = storageKey;
        page.contentType = contentType;
        page.sizeBytes = sizeBytes;
        page.sha256 = sha256;
        page.status = PageStatus.UPLOADED;
        return page;
    }

    public void markAnalyzing() {
        this.status = PageStatus.ANALYZING;
    }

    public void markAnalyzed(String ocrText) {
        this.status = PageStatus.ANALYZED;
        this.ocrText = ocrText;
    }

    public void markFailed() {
        this.status = PageStatus.FAILED;
    }

    /** 원본 사진을 지운 뒤 호출 */
    public void markOriginalDeleted() {
        this.storageKey = null;
        this.originalDeletedAt = Instant.now();
    }

    public boolean isOwnedBy(String userId) {
        return owner.getId().equals(userId);
    }
}

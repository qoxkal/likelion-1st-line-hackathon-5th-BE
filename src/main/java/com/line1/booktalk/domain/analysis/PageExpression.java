package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.domain.book.BookPage;
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
 * 페이지 분석으로 찾은 표현 하나.
 * 명세서의 vocabulary / vocabulary_translations / page_vocabulary 테이블은 담당이 정해지지 않아서,
 * 우선 분석 결과를 이 테이블에 그대로 저장한다. 테이블 설계가 정해지면 옮긴다.
 */
@Getter
@Entity
@Table(name = "page_expressions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageExpression extends BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private BookPage page;

    @Column(nullable = false, length = 50)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpressionType type;

    /** 쉬운 한국어 뜻 */
    @Column(length = 500)
    private String definition;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Language translationLanguage;

    /** 부모 모국어 번역 */
    @Column(length = 500)
    private String translation;

    /** 그림책 안에서 이 표현이 나온 문장 */
    @Column(length = 500)
    private String sourceSentence;

    /** AI가 표현을 맞게 찾았다고 보는 정도 (0~1) */
    private Double confidence;

    public static PageExpression create(BookPage page, String text, ExpressionType type, String definition,
                                        Language translationLanguage, String translation,
                                        String sourceSentence, Double confidence) {
        PageExpression expression = new PageExpression();
        expression.id = IdGenerator.generate(IdGenerator.EXPRESSION);
        expression.page = page;
        expression.text = text;
        expression.type = type;
        expression.definition = definition;
        expression.translationLanguage = translationLanguage;
        expression.translation = translation;
        expression.sourceSentence = sourceSentence;
        expression.confidence = confidence;
        return expression;
    }

    /** DEMO 도서의 표현은 모든 로그인 사용자가, 사용자 도서의 표현은 소유자만 쓸 수 있다. */
    public boolean isAccessibleBy(String userId) {
        return page.getBook().isDemo() || page.isOwnedBy(userId);
    }
}

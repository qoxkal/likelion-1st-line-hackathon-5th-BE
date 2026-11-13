package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.IdGenerator;
import com.line1.booktalk.global.common.Language;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 표현의 다른 언어 번역.
 * PageExpression에는 분석할 때 고른 언어 번역 하나만 있어서, 다른 언어 번역은 여기에 따로 둔다.
 * (지금은 시연용 도서 단어의 zh-CN, en 번역을 넣어 둔다)
 */
@Getter
@Entity
@Table(name = "expression_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"expression_id", "language"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpressionTranslation extends BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expression_id", nullable = false)
    private PageExpression expression;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Language language;

    @Column(nullable = false, length = 500)
    private String text;

    public static ExpressionTranslation create(PageExpression expression, Language language, String text) {
        ExpressionTranslation t = new ExpressionTranslation();
        t.id = IdGenerator.generate(IdGenerator.EXPRESSION_TRANSLATION);
        t.expression = expression;
        t.language = language;
        t.text = text;
        return t;
    }

    public void changeText(String text) {
        this.text = text;
    }
}

package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.global.common.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpressionTranslationRepository extends JpaRepository<ExpressionTranslation, String> {
    Optional<ExpressionTranslation> findByExpressionIdAndLanguage(String expressionId, Language language);
}

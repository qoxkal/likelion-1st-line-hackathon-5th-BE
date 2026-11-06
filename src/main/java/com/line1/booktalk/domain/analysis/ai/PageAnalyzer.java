package com.line1.booktalk.domain.analysis.ai;

import com.line1.booktalk.domain.analysis.ExpressionType;
import com.line1.booktalk.global.common.Language;

import java.util.List;

/**
 * 그림책 페이지 사진을 AI로 분석하는 부분.
 * 지금은 FakePageAnalyzer(가짜 결과)를 쓰고, AI 모델이 정해지면 이 인터페이스의 진짜 구현을 추가한다.
 * 사진 한 장으로 글자 읽기, 표현 찾기, 번역을 한 번에 처리한다 (해커톤 단순화 4번).
 */
public interface PageAnalyzer {

    Result analyze(Input input);

    /** 어떤 모델과 프롬프트로 분석했는지 기록용 */
    String modelName();

    String promptVersion();

    record Input(byte[] image, String contentType, int childAge, Language explanationLanguage) {
    }

    record Result(String ocrText, List<Expression> expressions) {
    }

    record Expression(String text, ExpressionType type, String definition, String translation,
                      String sourceSentence, Double confidence) {
    }
}

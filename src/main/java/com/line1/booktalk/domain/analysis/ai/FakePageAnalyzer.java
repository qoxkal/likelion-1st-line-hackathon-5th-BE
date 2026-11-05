package com.line1.booktalk.domain.analysis.ai;

import com.line1.booktalk.domain.analysis.ExpressionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 모델이 정해지기 전까지 쓰는 가짜 분석기.
 * 실제 분석처럼 몇 초 기다린 뒤 정해진 결과를 돌려준다. 프론트에서 분석 흐름(대기 → 진행 → 완료)을
 * 미리 연결해볼 수 있게 하는 용도다. 진짜 AI 구현을 추가하면 이 클래스는 지운다.
 */
@Component
public class FakePageAnalyzer implements PageAnalyzer {

    @Override
    public Result analyze(Input input) {
        sleep(3000);
        String lang = input.explanationLanguage().getCode();
        return new Result(
                "토끼는 커다란 당근을 보고 가슴이 두근두근 했어요. 토끼는 깡충깡충 뛰어갔어요.",
                List.of(
                        new Expression("두근두근", ExpressionType.MIMETIC,
                                "가슴이 자꾸 세게 뛰는 모양이에요.",
                                "[" + lang + "] (가짜 번역) 두근두근",
                                "토끼는 커다란 당근을 보고 가슴이 두근두근 했어요.", 0.96),
                        new Expression("깡충깡충", ExpressionType.MIMETIC,
                                "두 발을 모으고 가볍게 자꾸 뛰는 모양이에요.",
                                "[" + lang + "] (가짜 번역) 깡충깡충",
                                "토끼는 깡충깡충 뛰어갔어요.", 0.94)
                )
        );
    }

    @Override
    public String modelName() {
        return "fake";
    }

    @Override
    public String promptVersion() {
        return "fake-v1";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

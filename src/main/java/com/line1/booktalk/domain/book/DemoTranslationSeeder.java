package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.analysis.ExpressionTranslation;
import com.line1.booktalk.domain.analysis.ExpressionTranslationRepository;
import com.line1.booktalk.domain.analysis.PageExpression;
import com.line1.booktalk.domain.analysis.PageExpressionRepository;
import com.line1.booktalk.global.common.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 시연용(DEMO) 도서 단어에 중국어(zh-CN), 영어(en) 번역을 넣는다.
 * DemoBookSeeder는 베트남어 번역만 넣어서, 아이 설명 언어를 바꾸면 항상 vi로 나오던 문제를 막는다.
 * 도서 시드가 끝난 뒤(서버 준비 완료 시점)에 돌고, 이미 있으면 내용만 맞춘다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoTranslationSeeder {

    /** 단어 → {zh-CN, en} */
    private static final Map<String, String[]> TRANSLATIONS = Map.ofEntries(
            Map.entry("두근두근", new String[]{"怦怦（心跳）", "pit-a-pat (heart pounding)"}),
            Map.entry("퐁당", new String[]{"扑通（落水声）", "plop"}),
            Map.entry("엉엉", new String[]{"呜呜（大哭）", "boo-hoo (crying loudly)"}),
            Map.entry("반짝반짝", new String[]{"一闪一闪", "twinkle twinkle"}),
            Map.entry("눈이 휘둥그레지다", new String[]{"惊得瞪大眼睛", "eyes go wide (in surprise)"}),
            Map.entry("첨벙첨벙", new String[]{"哗啦哗啦（踩水声）", "splish-splash"}),
            Map.entry("엉금엉금", new String[]{"慢吞吞地爬", "crawling slowly"}),
            Map.entry("꾸벅꾸벅", new String[]{"打瞌睡（一点一点地点头）", "nodding off"}),
            Map.entry("깡충깡충", new String[]{"蹦蹦跳跳", "hop hop (hopping)"}),
            Map.entry("살금살금", new String[]{"蹑手蹑脚", "tiptoeing quietly"}),
            Map.entry("데굴데굴", new String[]{"骨碌碌（滚动）", "rolling round and round"}),
            Map.entry("구불구불", new String[]{"弯弯曲曲", "winding, zigzag"}),
            Map.entry("꿀꺽", new String[]{"咕咚（一口吞下）", "gulp"}),
            Map.entry("아등바등", new String[]{"拼命挣扎", "struggling hard"}),
            Map.entry("영차", new String[]{"嘿哟（一起用力）", "heave-ho"})
    );

    private final PageExpressionRepository expressionRepository;
    private final ExpressionTranslationRepository translationRepository;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        int saved = 0;
        for (PageExpression e : expressionRepository.findAllByPageBookSource(BookSource.DEMO)) {
            String[] t = TRANSLATIONS.get(e.getText());
            if (t == null) continue;
            saved += upsert(e, Language.ZH_CN, t[0]) + upsert(e, Language.EN, t[1]);
        }
        log.info("DEMO 단어 번역(zh-CN, en) 확인 새로 넣은 것={}", saved);
    }

    private int upsert(PageExpression e, Language language, String text) {
        var existing = translationRepository.findByExpressionIdAndLanguage(e.getId(), language);
        if (existing.isPresent()) {
            if (!existing.get().getText().equals(text)) existing.get().changeText(text);
            return 0;
        }
        translationRepository.save(ExpressionTranslation.create(e, language, text));
        return 1;
    }
}

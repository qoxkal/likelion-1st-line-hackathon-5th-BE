package com.line1.booktalk.domain.speech;

import com.line1.booktalk.domain.ai.LearningAiClient;
import com.line1.booktalk.domain.analysis.PageExpression;
import com.line1.booktalk.domain.analysis.PageExpressionRepository;
import com.line1.booktalk.domain.book.BookSource;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 서버가 켜지면 시연용(DEMO) 책의 단어와 예문 음성을 미리 만들어 DB에 저장한다.
 * 이미 만든 건 건너뛰고, 하루 한도가 떨어지면 멈춘다. 다음에 서버가 켜질 때 남은 것부터 이어서 만든다.
 * 서버 시작을 막지 않도록 별도 스레드에서 천천히 돈다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoTtsWarmer {

    /** 분당 요청 한도에 걸리지 않게 한 문장씩 쉬어 가며 만든다 */
    private static final long PAUSE_MS = 7_000;

    private final PageExpressionRepository expressionRepository;
    private final TtsService ttsService;
    private final LearningAiClient aiClient;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!aiClient.isRemote()) return;
        Thread thread = new Thread(this::run, "demo-tts-warmer");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        Set<String> texts = new LinkedHashSet<>();
        for (PageExpression e : expressionRepository.findAllByPageBookSource(BookSource.DEMO)) {
            texts.add(e.getText().trim());
        }
        for (PageExpression e : expressionRepository.findAllByPageBookSource(BookSource.DEMO)) {
            if (e.getSourceSentence() != null && !e.getSourceSentence().isBlank()) texts.add(e.getSourceSentence().trim());
        }
        int made = 0, skipped = 0;
        for (String text : texts) {
            if (text.length() > 300) continue;
            if (ttsService.isCached(text)) { skipped++; continue; }
            try {
                ttsService.getOrCreate(text, 1.0);
                made++;
                Thread.sleep(PAUSE_MS);
            } catch (BusinessException ex) {
                if (ex.getErrorCode() == ErrorCode.AI_QUOTA_EXCEEDED) {
                    log.warn("시연용 음성 미리 만들기 중단: 오늘 TTS 한도 초과 (만든 것 {}, 남은 것 {})",
                            made, texts.size() - made - skipped);
                    return;
                }
                log.warn("시연용 음성 만들기 실패 text={} reason={}", text, ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ex) {
                log.warn("시연용 음성 만들기 실패 text={}", text, ex);
            }
        }
        log.info("시연용 음성 미리 만들기 끝: 새로 만든 것 {}, 이미 있던 것 {}, 전체 {}", made, skipped, texts.size());
    }
}

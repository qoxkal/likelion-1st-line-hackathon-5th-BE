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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /** 지금 만드는 중인지 */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** 전부 만들었는지 (다 만들었으면 더 확인하지 않는다) */
    private volatile boolean done = false;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        start();
    }

    /**
     * 한도가 떨어져서 멈췄으면 20분마다 다시 시도한다.
     * (Gemini 하루 한도는 미국 서부 자정, 한국 시간으로 오후 5시쯤 다시 생긴다)
     */
    @Scheduled(initialDelay = 20, fixedDelay = 20, timeUnit = TimeUnit.MINUTES)
    public void retry() {
        if (!done) start();
    }

    private void start() {
        if (!aiClient.isRemote() || !running.compareAndSet(false, true)) return;
        Thread thread = new Thread(() -> {
            try { run(); } finally { running.set(false); }
        }, "demo-tts-warmer");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        // 단어보다 그림책 예문이 더 길고 시연에서 많이 쓰여서, 예문을 먼저 만든다
        Set<String> texts = new LinkedHashSet<>();
        for (PageExpression e : expressionRepository.findAllByPageBookSource(BookSource.DEMO)) {
            if (e.getSourceSentence() != null && !e.getSourceSentence().isBlank()) texts.add(e.getSourceSentence().trim());
        }
        for (PageExpression e : expressionRepository.findAllByPageBookSource(BookSource.DEMO)) {
            texts.add(e.getText().trim());
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
        done = texts.stream().allMatch(ttsService::isCached);
        log.info("시연용 음성 미리 만들기 끝: 새로 만든 것 {}, 이미 있던 것 {}, 전체 {}", made, skipped, texts.size());
    }
}

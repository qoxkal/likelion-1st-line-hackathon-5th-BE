package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.domain.analysis.ai.PageAnalysisException;
import com.line1.booktalk.domain.analysis.ai.PageAnalyzer;
import com.line1.booktalk.domain.book.BookPage;
import com.line1.booktalk.global.common.Language;
import com.line1.booktalk.global.config.AsyncConfig;
import com.line1.booktalk.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.CancellationException;

/**
 * 분석 작업을 백그라운드에서 처리한다.
 * 상태 변경을 단계마다 짧은 트랜잭션으로 저장해서, 처리 도중에도 GET /analysis/{jobId}로 진행 상황이 보인다.
 *
 * QUEUED → RUNNING(READING_TEXT → EXTRACTING → TRANSLATING → SAVING) → COMPLETED
 *                                                                      ↘ FAILED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisWorker {

    private final AnalysisJobRepository analysisJobRepository;
    private final PageExpressionRepository pageExpressionRepository;
    private final PageAnalyzer pageAnalyzer;
    private final FileStorage fileStorage;
    private final TransactionTemplate transactionTemplate;

    /** 작업 요청 트랜잭션이 커밋된 뒤, 별도 스레드에서 실행된다 */
    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisRequested(AnalysisRequestedEvent event) {
        process(event.jobId());
    }

    private record Prepared(String storageKey, String contentType, int childAge, Language language) {
    }

    private void process(String jobId) {
        String storageKey = null;
        try {
            // 1) 시작: RUNNING으로 바꾸고 분석에 필요한 정보 꺼내기
            Prepared prepared = transactionTemplate.execute(status -> {
                AnalysisJob job = getJob(jobId);
                job.start(pageAnalyzer.modelName(), pageAnalyzer.promptVersion());
                BookPage page = job.getPage();
                return new Prepared(page.getStorageKey(), page.getContentType(),
                        job.getChild().getAge(), job.getChild().getExplanationLanguage());
            });
            storageKey = prepared.storageKey();

            // 2) AI 호출 (오래 걸리는 부분이라 트랜잭션 밖에서)
            updateStage(jobId, AnalysisStage.EXTRACTING, 30);
            byte[] image = fileStorage.load(storageKey);
            PageAnalyzer.Result result = pageAnalyzer.analyze(new PageAnalyzer.Input(
                    image, prepared.contentType(), prepared.childAge(), prepared.language()));
            updateStage(jobId, AnalysisStage.TRANSLATING, 70);

            // 3) 결과 저장 후 완료
            transactionTemplate.executeWithoutResult(status -> {
                AnalysisJob job = getJob(jobId);
                job.moveTo(AnalysisStage.SAVING, 90);
                BookPage page = job.getPage();
                Language language = job.getChild().getExplanationLanguage();

                for (PageAnalyzer.Expression e : result.expressions()) {
                    pageExpressionRepository.save(PageExpression.create(
                            page, e.text(), e.type(), e.definition(),
                            language, e.translation(), e.sourceSentence(), e.confidence()));
                }
                page.getBook().addExpressionCount(result.expressions().size());
                page.markAnalyzed(result.ocrText());
                job.complete();
            });

            // 4) 성공했으니 원본 사진 바로 삭제 (해커톤 단순화 5번)
            fileStorage.delete(storageKey);
            transactionTemplate.executeWithoutResult(status ->
                    getJob(jobId).getPage().markOriginalDeleted());

        } catch (CancellationException e) {
            log.info("페이지 분석 취소 jobId={}", jobId);
        } catch (PageAnalysisException e) {
            // AI가 알려준 이유를 그대로 작업에 기록한다 (사진은 재시도용으로 남김)
            log.warn("페이지 분석 실패 jobId={} code={}", jobId, e.getCode());
            markFailed(jobId, e.getCode(), e.getUserMessage(), e.isRejected());
        } catch (Exception e) {
            // 실패하면 원본 사진은 재시도용으로 남겨둔다. 로그에는 jobId와 오류 종류만 남긴다
            log.error("페이지 분석 실패 jobId={} error={}", jobId, e.getClass().getSimpleName(), e);
            markFailed(jobId, "ANALYSIS_FAILED", "분석에 실패했어요. 잠시 후 다시 시도해 주세요.", false);
        }
    }

    private void updateStage(String jobId, AnalysisStage stage, int progress) {
        transactionTemplate.executeWithoutResult(status -> getJob(jobId).moveTo(stage, progress));
    }

    private void markFailed(String jobId, String code, String message, boolean rejected) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                AnalysisJob job = getJob(jobId);
                if (rejected) {
                    job.reject(code, message);
                } else {
                    job.fail(code, message);
                }
                job.getPage().markFailed();
            });
        } catch (Exception e) {
            log.error("분석 실패 상태 저장도 실패 jobId={}", jobId, e);
        }
    }

    private AnalysisJob getJob(String jobId) {
        return analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("분석 작업이 없습니다: " + jobId));
    }
}

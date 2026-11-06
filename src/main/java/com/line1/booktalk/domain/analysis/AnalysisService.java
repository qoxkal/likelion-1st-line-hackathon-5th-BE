package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.domain.analysis.dto.AnalysisJobResponse;
import com.line1.booktalk.domain.analysis.dto.AnalyzePageRequest;
import com.line1.booktalk.domain.analysis.dto.AnalyzePageResponse;
import com.line1.booktalk.domain.book.BookPage;
import com.line1.booktalk.domain.book.BookPageRepository;
import com.line1.booktalk.domain.book.PageStatus;
import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.domain.child.ChildService;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    /** 클라이언트가 상태를 다시 물어볼 간격 */
    public static final long POLL_AFTER_MS = 1500;

    private final AnalysisJobRepository analysisJobRepository;
    private final BookPageRepository bookPageRepository;
    private final ChildService childService;
    private final ApplicationEventPublisher eventPublisher;
    private final PageExpressionRepository pageExpressionRepository;

    /**
     * 분석 작업을 QUEUED로 만들고 바로 돌려준다.
     * 실제 분석은 커밋 후 AnalysisWorker가 따로 처리한다.
     */
    @Transactional
    public AnalyzePageResponse requestAnalysis(String userId, String bookId, AnalyzePageRequest request) {
        // 1) 내 페이지인지, 그 책의 페이지인지 확인 (아니면 404)
        BookPage page = bookPageRepository.findByIdAndOwnerId(request.pageId(), userId)
                .filter(p -> p.getBook().getId().equals(bookId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // 2) 이미 분석이 끝나 원본 사진을 지운 페이지는 다시 분석할 수 없다
        if (page.getStatus() == PageStatus.ANALYZED || page.getStorageKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "이미 분석이 끝난 페이지예요.");
        }

        // 3) 같은 페이지 작업이 진행 중이면 막는다
        if (analysisJobRepository.existsByPageIdAndStatusIn(page.getId(), JobStatus.IN_PROGRESS)) {
            throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_RUNNING);
        }

        // 4) 기준이 될 아이 (안 보내면 페이지를 올린 아이)
        Child child = (request.childId() == null || request.childId().isBlank())
                ? page.getChild()
                : childService.getOwnedChild(userId, request.childId());

        AnalysisJob job = analysisJobRepository.save(AnalysisJob.create(page, child));
        page.markAnalyzing();

        // 커밋이 끝난 뒤에 작업자가 시작되도록 이벤트로 넘긴다
        eventPublisher.publishEvent(new AnalysisRequestedEvent(job.getId()));

        return new AnalyzePageResponse(job.getId(), job.getStatus().name(), POLL_AFTER_MS);
    }

    @Transactional
    public AnalyzePageResponse requestAnalysisByPage(String userId, AnalyzePageRequest request) {
        BookPage page = bookPageRepository.findByIdAndOwnerId(request.pageId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return requestAnalysis(userId, page.getBook().getId(), request);
    }

    public AnalysisJobResponse getJob(String userId, String jobId) {
        AnalysisJob job = analysisJobRepository.findByIdAndPageOwnerId(jobId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return AnalysisJobResponse.from(job, POLL_AFTER_MS);
    }

    public PageAnalysisResult getResult(String userId, String pageId) {
        BookPage page = bookPageRepository.findByIdAndOwnerId(pageId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (page.getStatus() != PageStatus.ANALYZED) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "아직 분석이 완료되지 않았어요.");
        }
        List<ExpressionResult> expressions = pageExpressionRepository.findAllByPageIdOrderByCreatedAtAsc(pageId)
                .stream().map(e -> new ExpressionResult(e.getId(), e.getText(), e.getType().name(),
                        e.getDefinition(), new Translation(e.getTranslationLanguage().getCode(), e.getTranslation()),
                        e.getSourceSentence(), e.getConfidence())).toList();
        AnalysisJob job = analysisJobRepository.findFirstByPageIdAndStatusOrderByCreatedAtDesc(pageId, JobStatus.COMPLETED)
                .orElse(null);
        return new PageAnalysisResult(pageId, page.getOcrText(), "ko", expressions,
                new ModelInfo(job == null ? null : job.getModel(), job == null ? null : job.getPromptVersion()));
    }

    @Transactional
    public AnalysisJobResponse cancel(String userId, String jobId) {
        AnalysisJob job = analysisJobRepository.findByIdAndPageOwnerId(jobId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!job.isInProgress()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "취소할 수 있는 작업이 아니에요.");
        }
        job.cancel();
        if (job.getPage().getStatus() == PageStatus.ANALYZING) job.getPage().markFailed();
        return AnalysisJobResponse.from(job, POLL_AFTER_MS);
    }

    public record Translation(String language, String text) {}
    public record ExpressionResult(String id, String text, String type, String definition,
                                   Translation translation, String sourceSentence, Double confidence) {}
    public record ModelInfo(String provider, String promptVersion) {}
    public record PageAnalysisResult(String pageId, String ocrText, String language,
                                     List<ExpressionResult> expressions, ModelInfo model) {}
}

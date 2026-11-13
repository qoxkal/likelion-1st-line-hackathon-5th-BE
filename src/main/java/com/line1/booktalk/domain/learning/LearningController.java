package com.line1.booktalk.domain.learning;

import com.line1.booktalk.domain.analysis.ExpressionType;
import com.line1.booktalk.domain.analysis.PageExpression;
import com.line1.booktalk.domain.analysis.PageExpressionRepository;
import com.line1.booktalk.domain.book.BookPage;
import com.line1.booktalk.domain.book.BookPageRepository;
import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.domain.child.ChildService;
import com.line1.booktalk.domain.ai.LearningAiClient;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.IdGenerator;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
public class LearningController {
    private final LearningService service;

    @GetMapping("/vocabulary/{vocabularyId}")
    ApiResponse<VocabularyResponse> vocabulary(@LoginUser String userId, @PathVariable String vocabularyId) {
        return ApiResponse.of(service.vocabulary(userId, vocabularyId));
    }

    @PostMapping("/children/{childId}/bookmarks")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<BookmarkCreated> bookmark(@LoginUser String userId, @PathVariable String childId,
                                          @Valid @RequestBody BookmarkRequest request) {
        return ApiResponse.of(service.bookmark(userId, childId, request));
    }

    @DeleteMapping("/children/{childId}/bookmarks/{vocabularyId}")
    ApiResponse<Void> unbookmark(@LoginUser String userId, @PathVariable String childId,
                                 @PathVariable String vocabularyId) {
        service.unbookmark(userId, childId, vocabularyId);
        return ApiResponse.of(null);
    }

    @GetMapping("/children/{childId}/bookmarks")
    ApiResponse<List<BookmarkResponse>> bookmarks(@LoginUser String userId, @PathVariable String childId,
                                                  @RequestParam(required = false) ExpressionType type,
                                                  @RequestParam(required = false) LearningStatus status,
                                                  @RequestParam(defaultValue = "RECENT") BookmarkSort sort) {
        return ApiResponse.of(service.bookmarks(userId, childId, type, status, sort));
    }

    @PostMapping("/children/{childId}/learning-events")
    ApiResponse<AcceptedResponse> event(@LoginUser String userId, @PathVariable String childId,
                                        @Valid @RequestBody LearningEventRequest request) {
        return ApiResponse.of(new AcceptedResponse(service.event(userId, childId, request)));
    }

    @PostMapping("/children/{childId}/sentence-practices")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<SentencePracticeResponse> practice(@LoginUser String userId, @PathVariable String childId,
                                                   @Valid @RequestBody SentencePracticeRequest request) {
        return ApiResponse.of(service.practice(userId, childId, request));
    }

    @GetMapping("/children/{childId}/sentence-practices")
    ApiResponse<List<SentencePracticeResponse>> practices(@LoginUser String userId,
                                                          @PathVariable String childId,
                                                          @RequestParam(required = false) String vocabularyId,
                                                          @RequestParam(required = false) LocalDate from,
                                                          @RequestParam(required = false) LocalDate to) {
        return ApiResponse.of(service.practices(userId, childId, vocabularyId, from, to));
    }

    @GetMapping("/children/{childId}/reviews/today")
    ApiResponse<TodayReviews> today(@LoginUser String userId, @PathVariable String childId) {
        return ApiResponse.of(service.today(userId, childId));
    }

    @PostMapping("/reviews/{reviewId}/answers")
    ApiResponse<ReviewAnswerResponse> answer(@LoginUser String userId, @PathVariable String reviewId,
                                             @Valid @RequestBody ReviewAnswerRequest request) {
        return ApiResponse.of(service.answer(userId, reviewId, request));
    }

    @GetMapping("/children/{childId}/learning-summary")
    ApiResponse<LearningSummary> summary(@LoginUser String userId, @PathVariable String childId,
                                         @RequestParam(defaultValue = "WEEK") SummaryPeriod period) {
        return ApiResponse.of(service.summary(userId, childId, period));
    }

    public record BookmarkRequest(@NotBlank String vocabularyId, String sourcePageId) {}
    public record BookmarkCreated(String id, String vocabularyId, boolean created) {}
    public record Translation(String language, String text) {}
    public record SimilarExpression(String text, String nuance) {}
    public record SourceSentence(String ko, String translated) {}
    public record VocabularyResponse(String id, String text, String romanization, String type,
                                     String definition, Translation translation, List<String> usageSituations,
                                     SourceSentence sourceSentence, List<SimilarExpression> similarExpressions,
                                     List<String> parentTips, String childExplanation, String practiceExample) {}
    public record BookmarkResponse(String vocabularyId, String text, String type, String translation,
                                   String learningStatus, Instant reviewDueAt) {}
    public record LearningEventRequest(@NotBlank String eventId, @NotNull LearningEventType type,
                                       String vocabularyId, @NotNull Instant occurredAt,
                                       Map<String, Object> metadata) {}
    public record AcceptedResponse(boolean accepted) {}
    public record SentencePracticeRequest(@NotBlank String vocabularyId,
                                          @NotBlank @Size(max = 100) String sentence,
                                          @NotNull InputMethod inputMethod, @Size(max = 200) String context) {}
    public record Highlight(String text, String reason) {}
    public record SentencePracticeResponse(String id, String vocabularyId, String vocabularyText,
                                           String assessment, boolean meaningDelivered, boolean targetWordUsed,
                                           String originalSentence, String suggestedSentence, String feedback,
                                           List<Highlight> highlights, String promptVersion, Instant createdAt) {}
    public record ReviewItemResponse(String reviewId, String vocabularyId, String text, int stage,
                                     String questionType, String question, List<String> choices) {}
    public record TodayReviews(LocalDate date, int total, int estimatedSeconds, List<ReviewItemResponse> items) {}
    public record ReviewAnswerRequest(@NotNull Object answer, @PositiveOrZero long responseTimeMs,
                                      Confidence confidence) {}
    public record ReviewAnswerResponse(boolean correct, String feedback, Instant nextReviewAt, int stage) {}
    public record LearningSummary(String period, long discoveredWords, long bookmarkedWords,
                                  long sentencePractices, long conversationSessions,
                                  double reviewAccuracy, int streakDays) {}
}

enum LearningStatus { NEW, VIEWED, SENTENCE_DONE, CONVERSATION_DONE }
enum LearningEventType { WORD_VIEWED, AUDIO_PLAYED, SENTENCE_STARTED, SENTENCE_COMPLETED, DIALOGUE_COMPLETED }
enum InputMethod { KEYBOARD, SPEECH }
enum BookmarkSort { RECENT, REVIEW_DUE }
enum Confidence { LOW, MEDIUM, HIGH }
enum SummaryPeriod { WEEK, MONTH }

@Getter @Entity @Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "expression_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Bookmark extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "child_id") private Child child;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "expression_id") private PageExpression expression;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_page_id") private BookPage sourcePage;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private LearningStatus learningStatus;
    static Bookmark create(Child child, PageExpression expression, BookPage sourcePage) {
        Bookmark b = new Bookmark(); b.id = IdGenerator.generate(IdGenerator.BOOKMARK); b.child = child;
        b.expression = expression; b.sourcePage = sourcePage; b.learningStatus = LearningStatus.NEW; return b;
    }
    void advance(LearningStatus next) { if (next.ordinal() > learningStatus.ordinal()) learningStatus = next; }
}

interface BookmarkRepository extends org.springframework.data.jpa.repository.JpaRepository<Bookmark, String> {
    Optional<Bookmark> findByChildIdAndExpressionId(String childId, String expressionId);
    List<Bookmark> findAllByChildIdOrderByCreatedAtDesc(String childId);
    long countByChildIdAndCreatedAtAfter(String childId, Instant after);
}

@Getter @Entity @Table(name = "learning_events", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class LearningEvent extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @Column(nullable = false, unique = true, length = 80) private String eventId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Child child;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private LearningEventType type;
    @ManyToOne(fetch = FetchType.LAZY) private PageExpression expression;
    @Column(nullable = false) private Instant occurredAt;
    static LearningEvent create(Child c, LearningController.LearningEventRequest r, PageExpression e) {
        LearningEvent x = new LearningEvent(); x.id = IdGenerator.generate(IdGenerator.LEARNING_EVENT);
        x.eventId = r.eventId(); x.child = c; x.type = r.type(); x.expression = e; x.occurredAt = r.occurredAt(); return x;
    }
}
interface LearningEventRepository extends org.springframework.data.jpa.repository.JpaRepository<LearningEvent, String> {
    boolean existsByEventId(String eventId);
    long countDistinctExpressionByChildIdAndOccurredAtAfter(String childId, Instant after);
    List<LearningEvent> findAllByChildIdAndOccurredAtAfter(String childId, Instant after);
}

@Getter @Entity @Table(name = "sentence_practices") @NoArgsConstructor(access = AccessLevel.PROTECTED)
class SentencePractice extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Child child;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private PageExpression expression;
    @Column(nullable = false, length = 100) private String originalSentence;
    @Column(nullable = false, length = 100) private String suggestedSentence;
    @Column(nullable = false, length = 30) private String assessment;
    @Column(nullable = false) private boolean meaningDelivered;
    @Column(nullable = false) private boolean targetWordUsed;
    @Column(nullable = false, length = 500) private String feedback;
    /** 어떤 방식으로 피드백했는지 (local-rule-v1 또는 AI 서버) */
    @Column(length = 40) private String promptVersion;
    static SentencePractice create(Child c, PageExpression e, String original, String suggested,
                                   boolean targetUsed, String assessment, String feedback) {
        return create(c, e, original, suggested, targetUsed, targetUsed, assessment, feedback, "local-rule-v1");
    }
    static SentencePractice create(Child c, PageExpression e, String original, String suggested, boolean targetUsed,
                                   boolean meaningDelivered, String assessment, String feedback, String promptVersion) {
        SentencePractice s = new SentencePractice(); s.id = IdGenerator.generate(IdGenerator.SENTENCE_PRACTICE);
        s.child = c; s.expression = e; s.originalSentence = original; s.suggestedSentence = cut(suggested, 100);
        s.targetWordUsed = targetUsed; s.meaningDelivered = meaningDelivered; s.assessment = assessment;
        s.feedback = cut(feedback, 500); s.promptVersion = promptVersion; return s;
    }
    private static String cut(String text, int max) { return text.length() <= max ? text : text.substring(0, max); }
}
interface SentencePracticeRepository extends org.springframework.data.jpa.repository.JpaRepository<SentencePractice, String> {
    List<SentencePractice> findAllByChildIdOrderByCreatedAtDesc(String childId);
    long countByChildIdAndCreatedAtAfter(String childId, Instant after);
}

@Getter @Entity @Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "expression_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ReviewItem extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "child_id") private Child child;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "expression_id") private PageExpression expression;
    @Column(nullable = false) private int stage;
    @Column(nullable = false) private Instant dueAt;
    private Boolean lastCorrect;
    static ReviewItem create(Child c, PageExpression e) {
        ReviewItem r = new ReviewItem(); r.id = IdGenerator.generate(IdGenerator.REVIEW); r.child = c;
        r.expression = e; r.dueAt = Instant.now(); return r;
    }
    boolean answer(boolean correct) {
        lastCorrect = correct; stage = correct ? Math.min(stage + 1, 8) : Math.max(stage - 1, 0);
        long days = correct ? Math.max(1, (long) Math.pow(2, Math.max(0, stage - 1))) : 1;
        dueAt = Instant.now().plus(Duration.ofDays(days)); return correct;
    }
}
interface ReviewRepository extends org.springframework.data.jpa.repository.JpaRepository<ReviewItem, String> {
    Optional<ReviewItem> findByChildIdAndExpressionId(String childId, String expressionId);
    List<ReviewItem> findAllByChildIdAndDueAtLessThanEqualOrderByDueAtAsc(String childId, Instant dueAt);
    Optional<ReviewItem> findByIdAndChildUserId(String id, String userId);
    List<ReviewItem> findAllByChildIdAndUpdatedAtAfter(String childId, Instant after);
}

@Slf4j @Service @RequiredArgsConstructor @Transactional(readOnly = true)
class LearningService {
    private static final String AI_PROMPT_VERSION = "sentence-reviews-v1";
    /** 단어 설명은 표현마다 한 번만 AI에 물어보고 기억해 둔다 */
    private final Map<String, LearningAiClient.WordLesson> lessonCache = new ConcurrentHashMap<>();
    private final LearningAiClient aiClient;
    private final ChildService childService;
    private final PageExpressionRepository expressionRepository;
    private final BookPageRepository pageRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LearningEventRepository eventRepository;
    private final SentencePracticeRepository practiceRepository;
    private final ReviewRepository reviewRepository;

    LearningController.VocabularyResponse vocabulary(String userId, String id) {
        PageExpression e = ownedExpression(userId, id);
        LearningAiClient.WordLesson lesson = wordLesson(e);
        List<String> situations = lesson != null && lesson.situations() != null && !lesson.situations().isEmpty()
                ? lesson.situations() : List.of("그림책을 읽을 때", "아이와 일상 대화를 할 때");
        List<LearningController.SimilarExpression> similar = lesson != null && lesson.similarExpression() != null
                ? List.of(new LearningController.SimilarExpression(lesson.similarExpression(), lesson.difference())) : List.of();
        List<String> tips = lesson != null && lesson.childExplanation() != null
                ? List.of("아이에게는 이렇게 설명해 보세요: " + lesson.childExplanation(), "표현을 몸짓과 함께 말해 보세요.")
                : List.of("표현을 몸짓과 함께 말해 보세요.", "아이가 만든 문장을 한 번 더 확장해 주세요.");
        return new LearningController.VocabularyResponse(e.getId(), e.getText(), romanize(e.getText()), e.getType().name(),
                e.getDefinition(), new LearningController.Translation(e.getTranslationLanguage().getCode(), e.getTranslation()),
                situations, new LearningController.SourceSentence(e.getSourceSentence(), e.getTranslation()),
                similar, tips, lesson == null ? null : lesson.childExplanation(), lesson == null ? null : lesson.practiceExample());
    }

    /** AI 단어 설명. AI를 못 쓰거나 실패하면 null (기본 문구로 대신 보여준다) */
    private LearningAiClient.WordLesson wordLesson(PageExpression e) {
        if (!canUseAi(e)) return null;
        LearningAiClient.WordLesson cached = lessonCache.get(e.getId());
        if (cached != null) return cached;
        try {
            LearningAiClient.WordLesson lesson = aiClient.wordLesson(info(e));
            lessonCache.put(e.getId(), lesson);
            return lesson;
        } catch (BusinessException ex) {
            log.warn("AI 단어 설명 실패, 기본 문구 사용 expressionId={} reason={}", e.getId(), ex.getMessage());
            return null;
        }
    }

    private boolean canUseAi(PageExpression e) {
        return e.getTranslationLanguage() != null && aiClient.supports(e.getTranslationLanguage().getCode())
                && e.getDefinition() != null && !e.getDefinition().isBlank()
                && e.getTranslation() != null && !e.getTranslation().isBlank();
    }

    private LearningAiClient.ExpressionInfo info(PageExpression e) {
        return new LearningAiClient.ExpressionInfo(e.getText(), e.getDefinition(), e.getSourceSentence(),
                e.getTranslationLanguage().getCode(), e.getTranslation());
    }

    @Transactional LearningController.BookmarkCreated bookmark(String userId, String childId, LearningController.BookmarkRequest req) {
        Child child = childService.getOwnedChild(userId, childId); PageExpression exp = ownedExpression(userId, req.vocabularyId());
        Optional<Bookmark> existing = bookmarkRepository.findByChildIdAndExpressionId(childId, exp.getId());
        if (existing.isPresent()) return new LearningController.BookmarkCreated(existing.get().getId(), exp.getId(), false);
        BookPage source = req.sourcePageId() == null ? null : pageRepository.findByIdAndOwnerId(req.sourcePageId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Bookmark saved = bookmarkRepository.save(Bookmark.create(child, exp, source));
        reviewRepository.save(ReviewItem.create(child, exp));
        return new LearningController.BookmarkCreated(saved.getId(), exp.getId(), true);
    }

    @Transactional void unbookmark(String userId, String childId, String vocabularyId) {
        childService.getOwnedChild(userId, childId);
        bookmarkRepository.findByChildIdAndExpressionId(childId, vocabularyId).ifPresent(bookmarkRepository::delete);
        reviewRepository.findByChildIdAndExpressionId(childId, vocabularyId).ifPresent(reviewRepository::delete);
    }

    List<LearningController.BookmarkResponse> bookmarks(String userId, String childId, ExpressionType type,
                                                         LearningStatus status, BookmarkSort sort) {
        childService.getOwnedChild(userId, childId);
        Comparator<Bookmark> comparator = sort == BookmarkSort.REVIEW_DUE
                ? Comparator.comparing(b -> reviewRepository.findByChildIdAndExpressionId(childId, b.getExpression().getId())
                    .map(ReviewItem::getDueAt).orElse(Instant.MAX))
                : Comparator.comparing(Bookmark::getCreatedAt).reversed();
        return bookmarkRepository.findAllByChildIdOrderByCreatedAtDesc(childId).stream()
                .filter(b -> type == null || b.getExpression().getType() == type)
                .filter(b -> status == null || b.getLearningStatus() == status).sorted(comparator)
                .map(b -> new LearningController.BookmarkResponse(b.getExpression().getId(), b.getExpression().getText(),
                        b.getExpression().getType().name(), b.getExpression().getTranslation(), b.getLearningStatus().name(),
                        reviewRepository.findByChildIdAndExpressionId(childId, b.getExpression().getId()).map(ReviewItem::getDueAt).orElse(null)))
                .toList();
    }

    @Transactional boolean event(String userId, String childId, LearningController.LearningEventRequest req) {
        Child child = childService.getOwnedChild(userId, childId);
        if (eventRepository.existsByEventId(req.eventId())) return false;
        PageExpression exp = req.vocabularyId() == null ? null : ownedExpression(userId, req.vocabularyId());
        eventRepository.save(LearningEvent.create(child, req, exp));
        if (exp != null) bookmarkRepository.findByChildIdAndExpressionId(childId, exp.getId()).ifPresent(b -> b.advance(switch (req.type()) {
            case WORD_VIEWED, AUDIO_PLAYED, SENTENCE_STARTED -> LearningStatus.VIEWED;
            case SENTENCE_COMPLETED -> LearningStatus.SENTENCE_DONE;
            case DIALOGUE_COMPLETED -> LearningStatus.CONVERSATION_DONE;
        }));
        return true;
    }

    @Transactional LearningController.SentencePracticeResponse practice(String userId, String childId,
                                                                          LearningController.SentencePracticeRequest req) {
        Child child = childService.getOwnedChild(userId, childId); PageExpression exp = ownedExpression(userId, req.vocabularyId());
        String original = req.sentence().trim();
        if (canUseAi(exp)) {
            SentencePractice reviewed = aiPractice(child, exp, original);
            if (reviewed != null) {
                SentencePractice saved = practiceRepository.save(reviewed);
                bookmarkRepository.findByChildIdAndExpressionId(childId, exp.getId()).ifPresent(b -> b.advance(LearningStatus.SENTENCE_DONE));
                return response(saved);
            }
        }
        boolean used = original.contains(exp.getText());
        String suggested = original.replace(exp.getText() + "을 해요", exp.getText() + "해요")
                .replace(exp.getText() + "를 해요", exp.getText() + "해요");
        boolean natural = used && suggested.equals(original);
        String assessment = !used ? "TARGET_NOT_USED" : natural ? "GOOD" : "NEEDS_REVISION";
        String feedback = !used ? "목표 표현을 문장에 넣어 보세요." : natural ? "뜻이 잘 전달되고 자연스러워요." : "뜻은 정확해요. 흉내말 뒤에는 조사 없이 '-하다'를 붙이면 자연스러워요.";
        SentencePractice saved = practiceRepository.save(SentencePractice.create(child, exp, original, suggested, used, assessment, feedback));
        bookmarkRepository.findByChildIdAndExpressionId(childId, exp.getId()).ifPresent(b -> b.advance(LearningStatus.SENTENCE_DONE));
        return response(saved);
    }

    /** AI 문장 피드백. 실패하면 null (기존 규칙으로 대신 처리한다) */
    private SentencePractice aiPractice(Child child, PageExpression exp, String original) {
        LearningAiClient.ReviewResult r;
        try {
            LearningAiClient.SentenceReview review = aiClient.reviewSentence(original, info(exp));
            r = review.result();
        } catch (BusinessException ex) {
            log.warn("AI 문장 피드백 실패, 기존 규칙 사용 expressionId={} reason={}", exp.getId(), ex.getMessage());
            return null;
        }
        if (r == null || r.kind() == null) return null;
        boolean contains = original.contains(exp.getText());
        return switch (r.kind()) {
            case "natural" -> SentencePractice.create(child, exp, original, original, true, true, "GOOD",
                    "뜻이 잘 전달되고 자연스러워요.", AI_PROMPT_VERSION);
            case "polish" -> SentencePractice.create(child, exp, original,
                    blankOr(r.suggestedSentence(), original), true, true, "NEEDS_REVISION",
                    join(r.goodPoint(), r.correction()), AI_PROMPT_VERSION);
            case "meaning" -> SentencePractice.create(child, exp, original, original, contains, false, "MEANING_MISMATCH",
                    blankOr(r.hint(), "표현의 뜻을 다시 살펴보고 문장을 바꿔 보세요."), AI_PROMPT_VERSION);
            case "missing" -> SentencePractice.create(child, exp, original, original, false, false, "TARGET_NOT_USED",
                    blankOr(r.hint(), "목표 표현을 문장에 넣어 보세요."), AI_PROMPT_VERSION);
            default -> null;
        };
    }
    private static String blankOr(String text, String fallback) { return text == null || text.isBlank() ? fallback : text.trim(); }
    private static String join(String a, String b) {
        String x = a == null ? "" : a.trim(); String y = b == null ? "" : b.trim();
        return (x + " " + y).trim().isEmpty() ? "조금만 다듬으면 더 자연스러워요." : (x + " " + y).trim();
    }

    List<LearningController.SentencePracticeResponse> practices(String userId, String childId, String vocabularyId,
                                                                 LocalDate from, LocalDate to) {
        childService.getOwnedChild(userId, childId);
        return practiceRepository.findAllByChildIdOrderByCreatedAtDesc(childId).stream()
                .filter(p -> vocabularyId == null || p.getExpression().getId().equals(vocabularyId))
                .filter(p -> from == null || !p.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().isBefore(from))
                .filter(p -> to == null || !p.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().isAfter(to))
                .map(this::response).toList();
    }

    LearningController.TodayReviews today(String userId, String childId) {
        childService.getOwnedChild(userId, childId);
        List<LearningController.ReviewItemResponse> items = reviewRepository
                .findAllByChildIdAndDueAtLessThanEqualOrderByDueAtAsc(childId, Instant.now()).stream()
                .map(r -> new LearningController.ReviewItemResponse(r.getId(), r.getExpression().getId(),
                        r.getExpression().getText(), r.getStage(), "MEANING_CHOICE", "이 표현의 뜻은 무엇일까요?",
                        List.of(r.getExpression().getDefinition(), "반대 뜻", "관련 없는 뜻"))).toList();
        return new LearningController.TodayReviews(LocalDate.now(ZoneId.of("Asia/Seoul")), items.size(), items.size() * 30, items);
    }

    @Transactional LearningController.ReviewAnswerResponse answer(String userId, String reviewId, LearningController.ReviewAnswerRequest req) {
        ReviewItem item = reviewRepository.findByIdAndChildUserId(reviewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        String answer = req.answer() instanceof Collection<?> c ? String.join(" ", c.stream().map(String::valueOf).toList()) : String.valueOf(req.answer());
        boolean correct = answer.trim().equalsIgnoreCase(item.getExpression().getDefinition().trim()); item.answer(correct);
        return new LearningController.ReviewAnswerResponse(correct, correct ? "정답이에요!" : "표현의 뜻을 다시 살펴봐요.", item.getDueAt(), item.getStage());
    }

    LearningController.LearningSummary summary(String userId, String childId, SummaryPeriod period) {
        childService.getOwnedChild(userId, childId); Instant after = Instant.now().minus(Duration.ofDays(period == SummaryPeriod.WEEK ? 7 : 30));
        List<ReviewItem> reviews = reviewRepository.findAllByChildIdAndUpdatedAtAfter(childId, after);
        long answered = reviews.stream().filter(r -> r.getLastCorrect() != null).count();
        long correct = reviews.stream().filter(r -> Boolean.TRUE.equals(r.getLastCorrect())).count();
        List<LearningEvent> events = eventRepository.findAllByChildIdAndOccurredAtAfter(childId, after);
        long discovered = events.stream().map(e -> e.getExpression() == null ? null : e.getExpression().getId()).filter(Objects::nonNull).distinct().count();
        return new LearningController.LearningSummary(period.name(), discovered,
                bookmarkRepository.countByChildIdAndCreatedAtAfter(childId, after),
                practiceRepository.countByChildIdAndCreatedAtAfter(childId, after),
                events.stream().filter(e -> e.getType() == LearningEventType.DIALOGUE_COMPLETED).count(),
                answered == 0 ? 0 : (double) correct / answered, streakDays(events));
    }

    private int streakDays(List<LearningEvent> events) {
        Set<LocalDate> days = new HashSet<>(); for (LearningEvent e : events) days.add(e.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate());
        int streak = 0; LocalDate d = LocalDate.now(ZoneOffset.UTC); while (days.contains(d)) { streak++; d = d.minusDays(1); } return streak;
    }
    private PageExpression ownedExpression(String userId, String id) {
        PageExpression e = expressionRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!e.getPage().isOwnedBy(userId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); return e;
    }
    private LearningController.SentencePracticeResponse response(SentencePractice p) {
        String version = p.getPromptVersion() == null ? "local-rule-v1" : p.getPromptVersion();
        boolean local = "local-rule-v1".equals(version);
        List<LearningController.Highlight> highlights = p.getOriginalSentence().equals(p.getSuggestedSentence()) ? List.of()
                : local ? List.of(new LearningController.Highlight(p.getExpression().getText() + "을/를 해요", "불필요한 목적격 조사"))
                : List.of(new LearningController.Highlight(p.getSuggestedSentence(), "이렇게 고치면 더 자연스러워요"));
        return new LearningController.SentencePracticeResponse(p.getId(), p.getExpression().getId(), p.getExpression().getText(),
                p.getAssessment(), p.isMeaningDelivered(), p.isTargetWordUsed(), p.getOriginalSentence(), p.getSuggestedSentence(),
                p.getFeedback(), highlights, version, p.getCreatedAt());
    }
    private String romanize(String text) { return text; }
}

package com.line1.booktalk.domain.conversation;

import com.line1.booktalk.domain.analysis.PageExpression;
import com.line1.booktalk.domain.analysis.PageExpressionRepository;
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

import java.time.Instant;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService service;

    @PostMapping("/children/{childId}/conversation-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<SessionResponse> start(@LoginUser String userId, @PathVariable String childId,
                                       @Valid @RequestBody StartRequest request) {
        return ApiResponse.of(service.start(userId, childId, request));
    }

    @PostMapping("/conversation-sessions/{sessionId}/messages")
    ApiResponse<MessageExchange> message(@LoginUser String userId, @PathVariable String sessionId,
                                         @Valid @RequestBody MessageRequest request) {
        return ApiResponse.of(service.message(userId, sessionId, request));
    }

    @PostMapping("/conversation-sessions/{sessionId}/complete")
    ApiResponse<SessionResponse> complete(@LoginUser String userId, @PathVariable String sessionId) {
        return ApiResponse.of(service.complete(userId, sessionId));
    }

    @GetMapping("/conversation-sessions/{sessionId}")
    ApiResponse<SessionResponse> get(@LoginUser String userId, @PathVariable String sessionId) {
        return ApiResponse.of(service.get(userId, sessionId));
    }

    public record StartRequest(@NotBlank String vocabularyId, @NotBlank @Size(max = 100) String scene,
                               Difficulty difficulty, Boolean translationHintEnabled) {}
    public record MessageRequest(@NotBlank String clientMessageId, @NotBlank @Size(max = 300) String text,
                                 @NotNull InputMethod inputMethod, String transcriptionId) {}
    public record Persona(String name, int age) {}
    public record Mission(String targetText, int maxTurns, Boolean targetUsed, Integer progress, Boolean completed) {}
    /** hint: 보호자가 어떻게 답하면 좋을지 알려주는 힌트, feedback: 방금 보호자 답에 대한 피드백, accepted: 답이 통과됐는지 (보호자 메시지만) */
    public record MessageResponse(String id, String role, String text, String audioUrl, String translationHint,
                                  String hint, String feedback, Boolean accepted, Instant createdAt) {}
    public record Summary(int turnCount, boolean targetWordUsed, String naturalness, String encouragement, String nextTip) {}
    public record SessionResponse(String id, String status, Persona persona, Mission mission,
                                  MessageResponse openingMessage, List<MessageResponse> messages, Summary summary) {}
    public record MessageExchange(MessageResponse userMessage, MessageResponse aiMessage,
                                  Mission mission, String sessionStatus) {}
}

enum ConversationStatus { ACTIVE, COMPLETED }
enum Difficulty { LEVEL_1, LEVEL_2 }
enum InputMethod { TEXT, SPEECH }
enum MessageRole { GUARDIAN, CHILD_AI }

@Getter @Entity @Table(name = "conversation_sessions") @NoArgsConstructor(access = AccessLevel.PROTECTED)
class ConversationSession extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Child child;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private PageExpression expression;
    @Column(nullable = false, length = 100) private String scene;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Difficulty difficulty;
    @Column(nullable = false) private boolean translationHintEnabled;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConversationStatus status;
    @Column(nullable = false) private int turnCount;
    @Column(nullable = false) private boolean targetUsed;
    @Column(length = 500) private String encouragement;
    @Column(length = 500) private String nextTip;
    static ConversationSession create(Child c, PageExpression e, ConversationController.StartRequest r) {
        ConversationSession s = new ConversationSession(); s.id = IdGenerator.generate(IdGenerator.CONVERSATION_SESSION);
        s.child = c; s.expression = e; s.scene = r.scene().trim(); s.difficulty = r.difficulty() == null ? Difficulty.LEVEL_1 : r.difficulty();
        s.translationHintEnabled = Boolean.TRUE.equals(r.translationHintEnabled()); s.status = ConversationStatus.ACTIVE; return s;
    }
    /** 보호자 답이 통과되면 호출한다. 통과한 답이 MAX_TURNS개가 되면 대화가 끝난다 */
    void accept(boolean used, String feedback) {
        turnCount++; targetUsed |= used;
        if (turnCount >= ConversationService.MAX_TURNS) complete(feedback);
    }
    void complete() { complete(null); }
    void complete(String aiFeedback) {
        status = ConversationStatus.COMPLETED;
        encouragement = aiFeedback != null && !aiFeedback.isBlank() ? aiFeedback
                : targetUsed ? "목표 표현을 자연스럽게 대화에 사용했어요." : "끝까지 대화를 이어간 점이 좋아요.";
        nextTip = "아이의 답을 한 문장 더 확장해 주세요.";
    }
}

interface ConversationSessionRepository extends org.springframework.data.jpa.repository.JpaRepository<ConversationSession, String> {
    Optional<ConversationSession> findByIdAndChildUserId(String id, String userId);
    boolean existsByChildIdAndStatus(String childId, ConversationStatus status);
}

@Getter @Entity @Table(name = "conversation_messages", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "client_message_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ConversationMessage extends BaseEntity {
    @Id @Column(length = 40) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id") private ConversationSession session;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MessageRole role;
    @Column(name = "client_message_id", length = 80) private String clientMessageId;
    @Column(nullable = false, length = 500) private String text;
    @Column(length = 500) private String translationHint;
    /** 보호자가 어떻게 답하면 좋을지 (아이 메시지에 붙는다) */
    @Column(length = 500) private String hint;
    /** 보호자 답에 대한 피드백 (아이 메시지에 붙는다) */
    @Column(length = 500) private String feedback;
    /** 보호자 답이 통과됐는지 (보호자 메시지에만 쓴다) */
    private Boolean accepted;
    static ConversationMessage create(ConversationSession s, MessageRole role, String clientId, String text, String hint) {
        ConversationMessage m = new ConversationMessage(); m.id = IdGenerator.generate(IdGenerator.CONVERSATION_MESSAGE);
        m.session = s; m.role = role; m.clientMessageId = clientId; m.text = cut(text); m.translationHint = cut(hint); return m;
    }
    static ConversationMessage child(ConversationSession s, String text, String translationHint, String hint, String feedback) {
        ConversationMessage m = create(s, MessageRole.CHILD_AI, null, text, translationHint);
        m.hint = cut(hint); m.feedback = cut(feedback); return m;
    }
    void markAccepted(boolean accepted) { this.accepted = accepted; }
    private static String cut(String text) { return text == null || text.length() <= 500 ? text : text.substring(0, 500); }
}
interface ConversationMessageRepository extends org.springframework.data.jpa.repository.JpaRepository<ConversationMessage, String> {
    List<ConversationMessage> findAllBySessionIdOrderByCreatedAtAsc(String sessionId);
    Optional<ConversationMessage> findBySessionIdAndClientMessageId(String sessionId, String clientMessageId);
}

@Slf4j @Service @RequiredArgsConstructor @Transactional(readOnly = true)
class ConversationService {
    /** 보호자 답이 몇 번 통과되면 대화가 끝나는지 (AI 서버와 같은 값) */
    static final int MAX_TURNS = 4;
    private static final String CLOSING_LINE = "고마워! 오늘 이야기 정말 재밌었어.";
    private final LearningAiClient aiClient;
    private final ChildService childService;
    private final PageExpressionRepository expressionRepository;
    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;

    @Transactional ConversationController.SessionResponse start(String userId, String childId, ConversationController.StartRequest req) {
        Child child = childService.getOwnedChild(userId, childId);
        if (sessionRepository.existsByChildIdAndStatus(childId, ConversationStatus.ACTIVE))
            throw new BusinessException(ErrorCode.ACTIVE_CONVERSATION_EXISTS);
        PageExpression exp = ownedExpression(userId, req.vocabularyId());
        ConversationSession session = sessionRepository.save(ConversationSession.create(child, exp, req));
        String line = opening(req.scene()); String hint = "'" + exp.getText() + "'을(를) 넣어서 대답해 보세요.";
        if (canUseAi(exp)) {
            try {
                LearningAiClient.DialogueOpening ai = aiClient.openDialogue(info(exp), session.getScene());
                line = ai.childLine(); hint = ai.hint();
            } catch (BusinessException ex) {
                log.warn("AI 대화 시작 실패, 기본 문장 사용 expressionId={} reason={}", exp.getId(), ex.getMessage());
            }
        }
        ConversationMessage opening = messageRepository.save(ConversationMessage.child(session, line,
                session.isTranslationHintEnabled() ? exp.getTranslation() : null, hint, null));
        return response(session, opening);
    }

    @Transactional ConversationController.MessageExchange message(String userId, String sessionId, ConversationController.MessageRequest req) {
        ConversationSession session = ownedSession(userId, sessionId);
        if (session.getStatus() != ConversationStatus.ACTIVE) throw new BusinessException(ErrorCode.INVALID_STATE, "이미 끝난 대화예요.");
        Optional<ConversationMessage> duplicate = messageRepository.findBySessionIdAndClientMessageId(sessionId, req.clientMessageId());
        if (duplicate.isPresent()) {
            List<ConversationMessage> all = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
            int idx = all.indexOf(duplicate.get()); ConversationMessage ai = idx >= 0 && idx + 1 < all.size() ? all.get(idx + 1) : duplicate.get();
            return exchange(session, duplicate.get(), ai);
        }
        PageExpression exp = session.getExpression();
        String answer = req.text().trim();
        boolean used = answer.contains(exp.getText());
        List<ConversationMessage> history = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        String lastChildLine = lastChildLine(history);

        // 기본(AI를 못 쓸 때): 답은 모두 통과, 미리 정해 둔 아이 대사
        String status = session.getTurnCount() + 1 >= MAX_TURNS ? "complete" : "continue";
        String childLine = used ? "응! 나도 " + exp.getText() + "해! 그다음엔 어떻게 될까?" : "그다음에는 어떤 일이 일어날까?";
        String hint = "'" + exp.getText() + "'을(를) 넣어서 대답해 보세요.";
        String feedback = used ? "목표 표현을 잘 썼어요!" : "좋아요. 다음에는 '" + exp.getText() + "'도 넣어 보세요.";
        if (canUseAi(exp)) {
            try {
                LearningAiClient.DialogueTurn ai = aiClient.answerDialogue(info(exp), session.getScene(),
                        acceptedExchanges(history), new LearningAiClient.Exchange(lastChildLine, answer));
                status = ai.status(); childLine = ai.childLine(); hint = ai.hint(); feedback = ai.feedback();
            } catch (BusinessException ex) {
                log.warn("AI 대화 응답 실패, 기본 문장 사용 sessionId={} reason={}", sessionId, ex.getMessage());
            }
        }

        boolean accepted = !"retry".equals(status);
        ConversationMessage user = ConversationMessage.create(session, MessageRole.GUARDIAN, req.clientMessageId(), answer, null);
        user.markAccepted(accepted);
        messageRepository.save(user);
        String aiText = switch (status) {
            case "retry" -> lastChildLine;             // 같은 질문을 다시 한다
            case "complete" -> CLOSING_LINE;
            default -> childLine == null || childLine.isBlank() ? "그다음에는 어떤 일이 일어날까?" : childLine;
        };
        ConversationMessage ai = messageRepository.save(ConversationMessage.child(session, aiText,
                session.isTranslationHintEnabled() ? exp.getTranslation() : null,
                "complete".equals(status) ? null : hint, feedback));
        if (accepted) {
            session.accept(used, feedback);
            if ("complete".equals(status) && session.getStatus() == ConversationStatus.ACTIVE) session.complete(feedback);
        }
        return exchange(session, user, ai);
    }

    @Transactional ConversationController.SessionResponse complete(String userId, String id) {
        ConversationSession s = ownedSession(userId, id); if (s.getStatus() == ConversationStatus.ACTIVE) s.complete(); return response(s, null);
    }
    ConversationController.SessionResponse get(String userId, String id) { return response(ownedSession(userId, id), null); }

    private ConversationController.MessageExchange exchange(ConversationSession s, ConversationMessage user, ConversationMessage ai) {
        return new ConversationController.MessageExchange(message(user), message(ai), mission(s), s.getStatus().name());
    }
    private ConversationController.SessionResponse response(ConversationSession s, ConversationMessage opening) {
        List<ConversationController.MessageResponse> messages = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(s.getId()).stream().map(this::message).toList();
        if (opening == null && !messages.isEmpty()) opening = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(s.getId()).getFirst();
        ConversationController.Summary summary = s.getStatus() == ConversationStatus.COMPLETED
                ? new ConversationController.Summary(s.getTurnCount(), s.isTargetUsed(), s.isTargetUsed() ? "GOOD" : "PRACTICE_NEEDED", s.getEncouragement(), s.getNextTip()) : null;
        return new ConversationController.SessionResponse(s.getId(), s.getStatus().name(), new ConversationController.Persona("민우", s.getChild().getAge()),
                mission(s),
                opening == null ? null : message(opening), messages, summary);
    }
    private ConversationController.MessageResponse message(ConversationMessage m) {
        return new ConversationController.MessageResponse(m.getId(), m.getRole().name(), m.getText(), null, m.getTranslationHint(),
                m.getHint(), m.getFeedback(), m.getAccepted(), m.getCreatedAt());
    }
    /** progress: 통과한 답 개수 기준 0~100 */
    private ConversationController.Mission mission(ConversationSession s) {
        boolean done = s.getStatus() == ConversationStatus.COMPLETED;
        int progress = done ? 100 : Math.min(100, s.getTurnCount() * 100 / MAX_TURNS);
        return new ConversationController.Mission(s.getExpression().getText(), MAX_TURNS, s.isTargetUsed(), progress, done);
    }
    /** 지금 보호자가 답해야 하는 아이의 마지막 말 */
    private String lastChildLine(List<ConversationMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--)
            if (history.get(i).getRole() == MessageRole.CHILD_AI) return history.get(i).getText();
        return "";
    }
    /** AI에 보낼 이전 대화: 통과한 보호자 답과 그 직전 아이의 말 */
    private List<LearningAiClient.Exchange> acceptedExchanges(List<ConversationMessage> history) {
        List<LearningAiClient.Exchange> result = new ArrayList<>(); String child = "";
        for (ConversationMessage m : history) {
            if (m.getRole() == MessageRole.CHILD_AI) child = m.getText();
            else if (Boolean.TRUE.equals(m.getAccepted())) result.add(new LearningAiClient.Exchange(child, m.getText()));
        }
        return result.size() > MAX_TURNS - 1 ? result.subList(result.size() - (MAX_TURNS - 1), result.size()) : result;
    }
    private boolean canUseAi(PageExpression e) {
        return e.getTranslationLanguage() != null && aiClient.supports(e.getTranslationLanguage().getCode())
                && e.getDefinition() != null && !e.getDefinition().isBlank();
    }
    private LearningAiClient.ExpressionInfo info(PageExpression e) {
        return new LearningAiClient.ExpressionInfo(e.getText(), e.getDefinition(), e.getSourceSentence(),
                e.getTranslationLanguage().getCode(), e.getTranslation());
    }
    private ConversationSession ownedSession(String userId, String id) { return sessionRepository.findByIdAndChildUserId(id, userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private PageExpression ownedExpression(String userId, String id) {
        PageExpression e = expressionRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!e.getPage().isOwnedBy(userId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); return e;
    }
    private String opening(String scene) { return scene + "에서 무슨 일이 생길 것 같아?"; }
}

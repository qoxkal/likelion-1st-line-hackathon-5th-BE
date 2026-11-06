package com.line1.booktalk.domain.conversation;

import com.line1.booktalk.domain.analysis.PageExpression;
import com.line1.booktalk.domain.analysis.PageExpressionRepository;
import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.domain.child.ChildService;
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
    public record MessageResponse(String id, String role, String text, String audioUrl, String translationHint, Instant createdAt) {}
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
    void addTurn(boolean used) { turnCount++; targetUsed |= used; if (targetUsed || turnCount >= 10) complete(); }
    void complete() { status = ConversationStatus.COMPLETED; encouragement = targetUsed ? "목표 표현을 자연스럽게 대화에 사용했어요." : "끝까지 대화를 이어간 점이 좋아요."; nextTip = "아이의 답을 한 문장 더 확장해 주세요."; }
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
    static ConversationMessage create(ConversationSession s, MessageRole role, String clientId, String text, String hint) {
        ConversationMessage m = new ConversationMessage(); m.id = IdGenerator.generate(IdGenerator.CONVERSATION_MESSAGE);
        m.session = s; m.role = role; m.clientMessageId = clientId; m.text = text; m.translationHint = hint; return m;
    }
}
interface ConversationMessageRepository extends org.springframework.data.jpa.repository.JpaRepository<ConversationMessage, String> {
    List<ConversationMessage> findAllBySessionIdOrderByCreatedAtAsc(String sessionId);
    Optional<ConversationMessage> findBySessionIdAndClientMessageId(String sessionId, String clientMessageId);
}

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
class ConversationService {
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
        ConversationMessage opening = messageRepository.save(ConversationMessage.create(session, MessageRole.CHILD_AI, null,
                opening(req.scene()), session.isTranslationHintEnabled() ? exp.getTranslation() : null));
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
        boolean used = req.text().contains(session.getExpression().getText());
        ConversationMessage user = messageRepository.save(ConversationMessage.create(session, MessageRole.GUARDIAN,
                req.clientMessageId(), req.text().trim(), null));
        String aiText = used ? "응! 나도 " + session.getExpression().getText() + "해!" : "그다음에는 어떤 일이 일어날까?";
        ConversationMessage ai = messageRepository.save(ConversationMessage.create(session, MessageRole.CHILD_AI, null, aiText,
                session.isTranslationHintEnabled() ? session.getExpression().getTranslation() : null));
        session.addTurn(used); return exchange(session, user, ai);
    }

    @Transactional ConversationController.SessionResponse complete(String userId, String id) {
        ConversationSession s = ownedSession(userId, id); if (s.getStatus() == ConversationStatus.ACTIVE) s.complete(); return response(s, null);
    }
    ConversationController.SessionResponse get(String userId, String id) { return response(ownedSession(userId, id), null); }

    private ConversationController.MessageExchange exchange(ConversationSession s, ConversationMessage user, ConversationMessage ai) {
        int progress = s.isTargetUsed() ? 100 : Math.min(90, s.getTurnCount() * 10);
        return new ConversationController.MessageExchange(message(user), message(ai),
                new ConversationController.Mission(s.getExpression().getText(), 10, s.isTargetUsed(), progress, s.getStatus() == ConversationStatus.COMPLETED), s.getStatus().name());
    }
    private ConversationController.SessionResponse response(ConversationSession s, ConversationMessage opening) {
        List<ConversationController.MessageResponse> messages = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(s.getId()).stream().map(this::message).toList();
        if (opening == null && !messages.isEmpty()) opening = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(s.getId()).getFirst();
        ConversationController.Summary summary = s.getStatus() == ConversationStatus.COMPLETED
                ? new ConversationController.Summary(s.getTurnCount(), s.isTargetUsed(), s.isTargetUsed() ? "GOOD" : "PRACTICE_NEEDED", s.getEncouragement(), s.getNextTip()) : null;
        return new ConversationController.SessionResponse(s.getId(), s.getStatus().name(), new ConversationController.Persona("민우", s.getChild().getAge()),
                new ConversationController.Mission(s.getExpression().getText(), 10, s.isTargetUsed(), s.isTargetUsed() ? 100 : s.getTurnCount() * 10, s.getStatus() == ConversationStatus.COMPLETED),
                opening == null ? null : message(opening), messages, summary);
    }
    private ConversationController.MessageResponse message(ConversationMessage m) {
        return new ConversationController.MessageResponse(m.getId(), m.getRole().name(), m.getText(), null, m.getTranslationHint(), m.getCreatedAt());
    }
    private ConversationSession ownedSession(String userId, String id) { return sessionRepository.findByIdAndChildUserId(id, userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private PageExpression ownedExpression(String userId, String id) {
        PageExpression e = expressionRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!e.getPage().isOwnedBy(userId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); return e;
    }
    private String opening(String scene) { return scene + "에서 무슨 일이 생길 것 같아?"; }
}

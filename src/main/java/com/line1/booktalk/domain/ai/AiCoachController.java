package com.line1.booktalk.domain.ai;

import com.line1.booktalk.domain.analysis.PageExpression;
import com.line1.booktalk.domain.analysis.PageExpressionRepository;
import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.domain.child.ChildService;
import com.line1.booktalk.domain.learning.LearningEvidenceService;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiCoachController {
    private final AiCoachService service;

    @PostMapping("/answers")
    public ApiResponse<AiOrchestrationClient.AnswerResponse> answer(@LoginUser String userId,
                                                                    @Valid @RequestBody AnswerRequest request) {
        return ApiResponse.of(service.answer(userId, request));
    }

    public record AnswerRequest(@NotBlank @Size(max = 500) String query, @NotBlank String childId,
                                List<String> vocabularyIds, RagMode mode) {}
    public enum RagMode { AUTO, HYBRID, GRAPH_LOCAL, GRAPH_GLOBAL, PLAN }
}

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AiCoachService {
    private static final int MAX_EXPRESSIONS = 50;
    private final ChildService childService;
    private final PageExpressionRepository expressionRepository;
    private final LearningEvidenceService learningEvidenceService;
    private final AiOrchestrationClient client;

    AiOrchestrationClient.AnswerResponse answer(String userId, AiCoachController.AnswerRequest request) {
        Child child = childService.getOwnedChild(userId, request.childId());
        Set<String> requestedIds = request.vocabularyIds() == null ? Set.of() : Set.copyOf(request.vocabularyIds());
        List<PageExpression> expressions = expressionRepository.findAllByPageOwnerIdOrderByCreatedAtDesc(userId).stream()
                .filter(expression -> requestedIds.isEmpty() || requestedIds.contains(expression.getId()))
                .limit(MAX_EXPRESSIONS).toList();
        if (!requestedIds.isEmpty() && expressions.size() != requestedIds.size())
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        List<AiOrchestrationClient.DocumentInput> documents = new ArrayList<>();
        documents.add(new AiOrchestrationClient.DocumentInput("profile:" + child.getId(),
                "아이 연령은 " + child.getAge() + "세이고 설명 언어는 "
                        + child.getExplanationLanguage().getCode() + "입니다.",
                "아이 프로필", "PROFILE", Map.of("age", child.getAge(),
                "language", child.getExplanationLanguage().getCode())));
        learningEvidenceService.recentForChild(child.getId(), 10).forEach(history ->
                documents.add(new AiOrchestrationClient.DocumentInput(history.id(), history.text(),
                        "학습 이력", "HISTORY", Map.of("occurredAt", history.occurredAt().toString()))));

        List<AiOrchestrationClient.NodeInput> nodes = new ArrayList<>();
        List<AiOrchestrationClient.EdgeInput> edges = new ArrayList<>();
        nodes.add(new AiOrchestrationClient.NodeInput("child:" + child.getId(), child.getAge() + "세 아이",
                "CHILD_PROFILE", Map.of("language", child.getExplanationLanguage().getCode())));
        for (PageExpression expression : expressions) {
            String content = "표현 '" + expression.getText() + "'은 " + nullToEmpty(expression.getDefinition())
                    + ". 번역은 '" + nullToEmpty(expression.getTranslation()) + "'이고 그림책 예문은 '"
                    + nullToEmpty(expression.getSourceSentence()) + "'입니다.";
            documents.add(new AiOrchestrationClient.DocumentInput(expression.getId(), content,
                    expression.getText(), "CONTENT", Map.of("type", expression.getType().name(),
                    "pageId", expression.getPage().getId())));
            String typeId = "type:" + expression.getType().name();
            String pageId = "page:" + expression.getPage().getId();
            nodes.add(new AiOrchestrationClient.NodeInput(expression.getId(), expression.getText(),
                    "EXPRESSION", Map.of("definition", nullToEmpty(expression.getDefinition()))));
            addNodeIfMissing(nodes, new AiOrchestrationClient.NodeInput(typeId, expression.getType().name(), "TYPE", Map.of()));
            addNodeIfMissing(nodes, new AiOrchestrationClient.NodeInput(pageId,
                    nullToEmpty(expression.getPage().getPageLabel()), "BOOK_PAGE", Map.of()));
            edges.add(new AiOrchestrationClient.EdgeInput(expression.getId(), typeId, "HAS_TYPE", 1.0));
            edges.add(new AiOrchestrationClient.EdgeInput(expression.getId(), pageId, "FROM_PAGE", 1.0));
            edges.add(new AiOrchestrationClient.EdgeInput("child:" + child.getId(), expression.getId(), "LEARNING_TARGET", 0.8));
        }
        addSameTypeEdges(expressions, edges);
        String route = request.mode() == null || request.mode() == AiCoachController.RagMode.AUTO
                ? null : request.mode().name();
        return client.answer(new AiOrchestrationClient.AnswerRequest(request.query().trim(), route,
                List.copyOf(documents), List.copyOf(nodes), List.copyOf(edges)));
    }

    private void addSameTypeEdges(List<PageExpression> expressions, List<AiOrchestrationClient.EdgeInput> edges) {
        for (int left = 0; left < expressions.size(); left++) {
            for (int right = left + 1; right < expressions.size(); right++) {
                if (expressions.get(left).getType() == expressions.get(right).getType())
                    edges.add(new AiOrchestrationClient.EdgeInput(expressions.get(left).getId(),
                            expressions.get(right).getId(), "SAME_TYPE", 0.6));
                if (edges.size() >= 500) return;
            }
        }
    }

    private void addNodeIfMissing(List<AiOrchestrationClient.NodeInput> nodes, AiOrchestrationClient.NodeInput candidate) {
        if (nodes.stream().noneMatch(node -> node.id().equals(candidate.id()))) nodes.add(candidate);
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }
}

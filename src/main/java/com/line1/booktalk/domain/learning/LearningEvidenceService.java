package com.line1.booktalk.domain.learning;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** AI 서비스에 원문 대신 최소한의 검증된 학습 이력만 전달한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningEvidenceService {
    private final SentencePracticeRepository practiceRepository;
    private final LearningEventRepository eventRepository;

    public List<LearningEvidence> recentForChild(String childId, int limit) {
        List<LearningEvidence> result = new ArrayList<>();
        practiceRepository.findAllByChildIdOrderByCreatedAtDesc(childId).stream().limit(limit).forEach(practice ->
                result.add(new LearningEvidence(
                        "practice:" + practice.getId(),
                        "목표 표현 '" + practice.getExpression().getText() + "' 문장 연습 결과는 "
                                + practice.getAssessment() + "이며 제안 문장은 '" + practice.getSuggestedSentence() + "'입니다.",
                        practice.getCreatedAt()
                )));
        if (result.size() < limit) {
            eventRepository.findAllByChildIdAndOccurredAtAfter(childId, Instant.EPOCH).stream()
                    .sorted((left, right) -> right.getOccurredAt().compareTo(left.getOccurredAt()))
                    .limit(limit - result.size())
                    .forEach(event -> result.add(new LearningEvidence(
                            "event:" + event.getId(),
                            "학습 활동 " + event.getType().name() + (event.getExpression() == null ? ""
                                    : "의 목표 표현은 '" + event.getExpression().getText() + "'입니다."),
                            event.getOccurredAt()
                    )));
        }
        return List.copyOf(result);
    }

    public record LearningEvidence(String id, String text, Instant occurredAt) {}
}

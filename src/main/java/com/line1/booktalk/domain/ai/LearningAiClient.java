package com.line1.booktalk.domain.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 서버(영채님 레포)의 학습 기능을 호출한다.
 * 문장 피드백, 단어 설명, 아이와 대화, 음성 합성(TTS), 음성 인식(STT).
 *
 * app.ai.features=remote 일 때만 진짜로 호출한다. fake면 각 서비스가 기존 규칙으로 대신 처리한다.
 * AI 서버의 학습 기능은 vi, zh-CN, en 세 언어만 받는다.
 */
@Slf4j
@Component
public class LearningAiClient {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("vi", "zh-CN", "en");

    private final RestClient restClient;
    private final boolean remote;

    public LearningAiClient(@Value("${app.ai.base-url}") String baseUrl,
                            @Value("${app.ai.features}") String mode,
                            @Value("${app.ai.connect-timeout-ms}") int connectTimeoutMs,
                            @Value("${app.ai.features-timeout-ms}") int readTimeoutMs) {
        this.remote = "remote".equalsIgnoreCase(mode);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Render 무료 서버는 잠들어 있다가 깨는 데 시간이 걸려서 넉넉히 기다린다
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** AI 서버를 진짜로 쓰는지 */
    public boolean isRemote() {
        return remote;
    }

    /** 이 언어로 학습 기능(문장 피드백, 단어 설명, 대화)을 쓸 수 있는지 */
    public boolean supports(String languageCode) {
        return remote && languageCode != null && SUPPORTED_LANGUAGES.contains(languageCode);
    }

    // ---------- 문장 피드백 ----------

    public SentenceReview reviewSentence(String sentence, ExpressionInfo e) {
        Map<String, Object> body = expressionBody(e);
        body.put("sentence", sentence);
        body.put("translation", e.translation());
        return post("/v1/sentence-reviews", body, SentenceReview.class);
    }

    // ---------- 단어 설명 ----------

    public WordLesson wordLesson(ExpressionInfo e) {
        Map<String, Object> body = expressionBody(e);
        body.put("translation", e.translation());
        return post("/v1/word-lessons", body, WordLesson.class);
    }

    // ---------- 아이와 대화 ----------

    public DialogueOpening openDialogue(ExpressionInfo e, String scene) {
        Map<String, Object> body = expressionBody(e);
        body.put("scene", scene);
        return post("/v1/dialogues/openings", body, DialogueOpening.class);
    }

    public DialogueTurn answerDialogue(ExpressionInfo e, String scene, List<Exchange> previous, Exchange current) {
        Map<String, Object> body = expressionBody(e);
        body.put("scene", scene);
        body.put("previous", previous.stream().map(Exchange::toBody).toList());
        body.put("current", current.toBody());
        return post("/v1/dialogues/turns", body, DialogueTurn.class);
    }

    // ---------- 음성 ----------

    /** 한국어 문장을 읽어 준 wav 파일을 받는다 */
    public byte[] synthesize(String text, double playbackRate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        body.put("playback_rate", playbackRate);
        return post("/v1/speech", body, byte[].class);
    }

    /** 녹음 파일을 글자로 바꾼다 */
    public Transcription transcribe(byte[] audio, String mimeType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("audio_base64", Base64.getEncoder().encodeToString(audio));
        body.put("mime_type", mimeType);
        return post("/v1/transcriptions", body, Transcription.class);
    }

    // ---------- 공통 ----------

    private Map<String, Object> expressionBody(ExpressionInfo e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("expression", e.expression());
        body.put("definition", e.definition());
        if (e.sourceSentence() != null && !e.sourceSentence().isBlank()) {
            body.put("source_sentence", e.sourceSentence());
        }
        body.put("language", e.language());
        return body;
    }

    private <T> T post(String uri, Map<String, Object> body, Class<T> type) {
        if (!remote) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }
        try {
            T result = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
            if (result == null) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "AI 결과를 받지 못했어요. 잠시 후 다시 시도해 주세요.");
            }
            return result;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("AI 서버 호출 실패 uri={} status={} body={}", uri, status, ex.getResponseBodyAsString());
            if (status == 429) {
                throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED);
            }
            if (status == 422) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "AI가 처리할 수 없는 입력이에요.");
            }
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        } catch (ResourceAccessException ex) {
            log.warn("AI 서버 연결 실패 또는 시간 초과 uri={}", uri);
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "AI 응답이 늦어지고 있어요. 잠시 후 다시 시도해 주세요.");
        } catch (RestClientException ex) {
            log.warn("AI 서버 응답 처리 실패 uri={}", uri, ex);
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }
    }

    // ---------- 주고받는 형식 (필드 이름은 AI 서버의 snake_case 그대로) ----------

    /** AI에 보낼 표현 정보 */
    public record ExpressionInfo(String expression, String definition, String sourceSentence,
                                 String language, String translation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SentenceReview(ReviewResult result) {
    }

    /** kind: polish(조금 다듬기) / meaning(뜻이 어긋남) / natural(자연스러움) / missing(표현을 안 씀) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewResult(String kind,
                               @JsonProperty("good_point") String goodPoint,
                               String correction,
                               @JsonProperty("suggested_sentence") String suggestedSentence,
                               String hint) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WordLesson(List<String> situations,
                             @JsonProperty("similar_expression") String similarExpression,
                             String difference,
                             @JsonProperty("child_explanation") String childExplanation,
                             @JsonProperty("practice_example") String practiceExample) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DialogueOpening(@JsonProperty("child_line") String childLine, String hint) {
    }

    /** status: retry(다시 말하기) / continue(다음 질문) / complete(대화 끝) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DialogueTurn(String status,
                               @JsonProperty("accepted_turns") int acceptedTurns,
                               @JsonProperty("child_line") String childLine,
                               String hint,
                               String feedback) {
    }

    public record Exchange(String childLine, String parentAnswer) {
        Map<String, Object> toBody() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("child_line", childLine);
            body.put("parent_answer", parentAnswer);
            return body;
        }
    }

    /** status: ok / no_speech */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transcription(String status, String text) {
    }
}

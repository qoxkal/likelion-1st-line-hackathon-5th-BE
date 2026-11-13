package com.line1.booktalk.domain.analysis.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.line1.booktalk.domain.analysis.ExpressionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 서버(영채님 레포)의 POST /v1/photo-analyses를 호출하는 진짜 분석기.
 * Cloud Vision이 글자를 읽고, Gemini가 표현·뜻·번역·출처 문장을 찾아준다.
 *
 * app.ai.page-analyzer=remote 일 때만 사용된다 (기본값 fake).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.page-analyzer", havingValue = "remote")
public class RemotePageAnalyzer implements PageAnalyzer {

    private final RestClient restClient;

    public RemotePageAnalyzer(@Value("${app.ai.base-url}") String baseUrl,
                              @Value("${app.ai.connect-timeout-ms}") int connectTimeoutMs,
                              @Value("${app.ai.page-analysis-timeout-ms}") int readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // 사진 분석은 글자 인식(최대 25초) + Gemini(최대 30초)라 일반 AI 호출보다 길게 기다린다
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public Result analyze(Input input) {
        Map<String, String> body = Map.of(
                "image_base64", Base64.getEncoder().encodeToString(input.image()),
                "language", input.explanationLanguage().getCode()
        );

        PhotoResponse response;
        try {
            response = restClient.post()
                    .uri("/v1/photo-analyses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PhotoResponse.class);
        } catch (RestClientResponseException e) {
            throw fromStatus(e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            // 연결 실패나 시간 초과 (Render 무료 서버가 잠들어 있을 때도 여기로 온다)
            throw new PageAnalysisException("AI_TIMEOUT",
                    "분석이 오래 걸리고 있어요. 잠시 후 다시 시도해 주세요.", false, true);
        } catch (RestClientException e) {
            // JSON 역직렬화 실패 등 HTTP 응답은 왔지만 계약과 다른 경우
            log.warn("AI 사진 분석 응답 파싱 실패", e);
            throw new PageAnalysisException("AI_RESPONSE_INVALID",
                    "분석 결과 형식이 올바르지 않아요. 잠시 후 다시 시도해 주세요.", false, true);
        }

        if (response == null || response.status() == null
                || !Set.of("ok", "no_text", "no_expressions").contains(response.status())) {
            throw new PageAnalysisException("AI_RESPONSE_INVALID",
                    "분석 결과를 받지 못했어요. 잠시 후 다시 시도해 주세요.", false, true);
        }

        // 사진에서 글자를 못 찾은 경우: 다시 찍어야 하므로 REJECTED
        if ("no_text".equals(response.status())) {
            throw new PageAnalysisException("IMAGE_QUALITY_LOW",
                    "글자를 찾지 못했어요. 글자가 잘 보이게 다시 찍어 주세요.", true, false);
        }

        // ok: 표현 있음 / no_expressions: 글자는 있지만 표현이 없음 (정상 완료, 0개)
        List<PhotoExpression> received = response.expressions() == null ? List.of() : response.expressions();
        if ("ok".equals(response.status()) && (received.isEmpty() || received.stream().anyMatch(this::isInvalid))) {
            throw new PageAnalysisException("AI_RESPONSE_INVALID",
                    "분석 결과 형식이 올바르지 않아요. 잠시 후 다시 시도해 주세요.", false, true);
        }
        List<Expression> expressions = received.stream()
                    .map(e -> new Expression(
                            e.expression(), toType(e.type()), e.definition(),
                            e.translation(), e.sourceSentence(), null))
                    .toList();

        // AI 서버는 인식한 글 전체를 돌려주지 않아서 ocrText는 비워 둔다
        return new Result(null, expressions);
    }

    @Override
    public String modelName() {
        return "booktalk-ai:vision+gemini";
    }

    @Override
    public String promptVersion() {
        return "photo-analyses-v1";
    }

    /** AI 서버의 HTTP 상태 코드를 작업 오류로 바꾼다 */
    private PageAnalysisException fromStatus(int status) {
        log.warn("AI 서버 사진 분석 실패 status={}", status);
        return switch (status) {
            case 429 -> new PageAnalysisException("AI_QUOTA_EXCEEDED",
                    "오늘 사용할 수 있는 분석 횟수를 모두 썼어요.", false, false);
            case 413, 415, 422 -> new PageAnalysisException("IMAGE_INVALID",
                    "사진을 분석할 수 없어요. 다른 사진으로 다시 찍어 주세요.", true, false);
            case 504 -> new PageAnalysisException("AI_TIMEOUT",
                    "분석이 오래 걸리고 있어요. 잠시 후 다시 시도해 주세요.", false, true);
            case 502 -> new PageAnalysisException("AI_RESPONSE_INVALID",
                    "분석 결과를 받지 못했어요. 잠시 후 다시 시도해 주세요.", false, true);
            default -> new PageAnalysisException("AI_PROVIDER_UNAVAILABLE",
                    "AI 서비스가 잠시 응답하지 않아요. 잠시 후 다시 시도해 주세요.", false, true);
        };
    }

    private ExpressionType toType(String type) {
        if (type == null) {
            return ExpressionType.MIMETIC;
        }
        return switch (type) {
            case "onomatopoeia" -> ExpressionType.ONOMATOPOEIA;
            case "idiom" -> ExpressionType.IDIOM;
            case "interjection" -> ExpressionType.INTERJECTION;
            default -> ExpressionType.MIMETIC;
        };
    }

    private boolean isInvalid(PhotoExpression expression) {
        return expression == null || expression.expression() == null || expression.expression().isBlank()
                || expression.type() == null || expression.type().isBlank()
                || expression.definition() == null || expression.definition().isBlank();
    }

    /** AI 서버 응답 형식 (필드 이름은 AI 서버의 snake_case 그대로) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotoResponse(String status, String language, List<PhotoExpression> expressions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotoExpression(String expression, String type, String definition, String translation,
                           @JsonProperty("source_sentence") String sourceSentence) {
    }
}

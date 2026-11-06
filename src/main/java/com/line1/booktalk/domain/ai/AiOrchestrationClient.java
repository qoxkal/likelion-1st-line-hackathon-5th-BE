package com.line1.booktalk.domain.ai;

import com.line1.booktalk.global.error.BusinessException;
import com.line1.booktalk.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class AiOrchestrationClient {
    private final RestClient restClient;
    private final boolean enabled;

    public AiOrchestrationClient(@Value("${app.ai.base-url}") String baseUrl,
                                 @Value("${app.ai.enabled}") boolean enabled,
                                 @Value("${app.ai.connect-timeout-ms}") int connectTimeoutMs,
                                 @Value("${app.ai.read-timeout-ms}") int readTimeoutMs) {
        this.enabled = enabled;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public AnswerResponse answer(AnswerRequest request) {
        if (!enabled) throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "AI 서비스가 비활성화되어 있어요.");
        try {
            AnswerResponse response = restClient.post().uri("/v1/answers").body(request).retrieve()
                    .body(AnswerResponse.class);
            if (response == null) throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "AI 검색 서비스에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    public record AnswerRequest(String query, String route, List<DocumentInput> documents,
                                List<NodeInput> nodes, List<EdgeInput> edges) {}
    public record DocumentInput(String id, String text, String title, String kind, Map<String, Object> metadata) {}
    public record NodeInput(String id, String label, String type, Map<String, Object> properties) {}
    public record EdgeInput(String source, String target, String type, double weight) {}
    public record PlanStep(int order, String tool, String reason) {}
    public record Evidence(String id, String text, String source, double score, Map<String, Object> metadata) {}
    public record Verification(boolean passed, double citationValidity, double evidenceCoverage,
                               List<String> unsupportedClaims) {}
    public record AnswerResponse(String answer, String route, List<String> citations, double confidence,
                                 boolean abstained, List<PlanStep> plan, List<Evidence> evidence,
                                 Verification verification, Map<String, Object> diagnostics) {}
}

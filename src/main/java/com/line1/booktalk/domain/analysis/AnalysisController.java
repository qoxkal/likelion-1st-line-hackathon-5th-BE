package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.domain.analysis.dto.AnalysisJobResponse;
import com.line1.booktalk.domain.analysis.dto.AnalyzePageRequest;
import com.line1.booktalk.domain.analysis.dto.AnalyzePageResponse;
import com.line1.booktalk.global.auth.LoginUser;
import com.line1.booktalk.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * 페이지 분석 시작. 바로 jobId를 돌려주고(202), 분석은 백그라운드에서 진행된다.
     * body: { "pageId": "bpg_...", "childId": "chd_..." (선택) }
     */
    @PostMapping("/books/{bookId}/pages/analyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AnalyzePageResponse> analyze(@LoginUser String userId,
                                                    @PathVariable String bookId,
                                                    @Valid @RequestBody AnalyzePageRequest request) {
        return ApiResponse.of(analysisService.requestAnalysis(userId, bookId, request));
    }

    /** 분석 작업 상태 조회 */
    @GetMapping("/analysis/{jobId}")
    public ApiResponse<AnalysisJobResponse> getJob(@LoginUser String userId,
                                                   @PathVariable String jobId) {
        return ApiResponse.of(analysisService.getJob(userId, jobId));
    }

    /** API 명세 경로와 호환되는 페이지 단위 분석 시작 */
    @PostMapping("/books/pages/{pageId}/analysis-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AnalyzePageResponse> analyzePage(@LoginUser String userId,
                                                        @PathVariable String pageId,
                                                        @Valid @RequestBody AnalyzePageJobRequest request) {
        AnalyzePageRequest legacy = new AnalyzePageRequest(pageId, request.childId());
        // page 소유권 검사는 서비스에서 수행하며 bookId는 페이지 자체의 bookId를 사용한다.
        return ApiResponse.of(analysisService.requestAnalysisByPage(userId, legacy));
    }

    @GetMapping("/analysis-jobs/{jobId}")
    public ApiResponse<AnalysisJobResponse> getAnalysisJob(@LoginUser String userId,
                                                           @PathVariable String jobId) {
        return ApiResponse.of(analysisService.getJob(userId, jobId));
    }

    @GetMapping("/books/pages/{pageId}/analysis")
    public ApiResponse<AnalysisService.PageAnalysisResult> getResult(@LoginUser String userId,
                                                                     @PathVariable String pageId) {
        return ApiResponse.of(analysisService.getResult(userId, pageId));
    }

    @PostMapping("/analysis-jobs/{jobId}/cancel")
    public ApiResponse<AnalysisJobResponse> cancel(@LoginUser String userId, @PathVariable String jobId) {
        return ApiResponse.of(analysisService.cancel(userId, jobId));
    }

    public record AnalyzePageJobRequest(@jakarta.validation.constraints.NotBlank String childId,
                                        Boolean force, List<String> features) {}
}

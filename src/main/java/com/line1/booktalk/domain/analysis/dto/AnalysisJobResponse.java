package com.line1.booktalk.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.line1.booktalk.domain.analysis.AnalysisJob;

import java.time.Instant;

/** GET /analysis/{jobId} 응답. 끝난 작업은 pollAfterMs가 없다 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisJobResponse(
        String id,
        String type,
        String pageId,
        String status,
        String stage,
        int progress,
        Long pollAfterMs,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static AnalysisJobResponse from(AnalysisJob job, long pollAfterMs) {
        return new AnalysisJobResponse(
                job.getId(),
                "PAGE_ANALYSIS",
                job.getPage().getId(),
                job.getStatus().name(),
                job.getStage() == null ? null : job.getStage().name(),
                job.getProgress(),
                job.isInProgress() ? pollAfterMs : null,
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}

package com.line1.booktalk.domain.analysis;

import java.util.Set;

/**
 * 분석 작업 상태 (명세서 6.1 작업 상태 머신)
 * QUEUED -> RUNNING -> COMPLETED / FAILED / REJECTED
 * QUEUED, RUNNING -> CANCELLED
 */
public enum JobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED;

    /** 아직 끝나지 않은 상태. 같은 페이지에 이 상태 작업이 있으면 새 작업을 막는다 */
    public static final Set<JobStatus> IN_PROGRESS = Set.of(QUEUED, RUNNING);
}

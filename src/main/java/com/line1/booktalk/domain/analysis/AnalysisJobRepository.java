package com.line1.booktalk.domain.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, String> {

    /** 같은 페이지에 진행 중인 작업이 있는지 (409 ANALYSIS_ALREADY_RUNNING) */
    boolean existsByPageIdAndStatusIn(String pageId, Collection<JobStatus> statuses);

    /** 소유권 검사용: 내 페이지의 작업만 조회 */
    Optional<AnalysisJob> findByIdAndPageOwnerId(String id, String ownerId);
}

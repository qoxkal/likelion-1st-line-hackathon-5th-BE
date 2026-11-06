package com.line1.booktalk.domain.analysis;

import com.line1.booktalk.domain.book.BookPage;
import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.global.common.BaseEntity;
import com.line1.booktalk.global.common.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 페이지 분석 작업. 요청이 오면 QUEUED로 만들고 jobId를 바로 돌려준 뒤,
 * @Async 작업자가 상태를 바꿔 가며 처리한다 (해커톤 단순화 3번).
 */
@Getter
@Entity
@Table(name = "analysis_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisJob extends BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private BookPage page;

    /** 연령과 설명 언어 기준이 되는 아이 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnalysisStage stage;

    /** 0 ~ 100 */
    @Column(nullable = false)
    private int progress;

    @Column(length = 50)
    private String model;

    @Column(length = 50)
    private String promptVersion;

    @Column(length = 50)
    private String errorCode;

    @Column(length = 500)
    private String errorMessage;

    private Instant startedAt;

    private Instant finishedAt;

    public static AnalysisJob create(BookPage page, Child child) {
        AnalysisJob job = new AnalysisJob();
        job.id = IdGenerator.generate(IdGenerator.ANALYSIS_JOB);
        job.page = page;
        job.child = child;
        job.status = JobStatus.QUEUED;
        job.progress = 0;
        return job;
    }

    public void start(String model, String promptVersion) {
        this.status = JobStatus.RUNNING;
        this.stage = AnalysisStage.READING_TEXT;
        this.progress = 10;
        this.model = model;
        this.promptVersion = promptVersion;
        this.startedAt = Instant.now();
    }

    public void moveTo(AnalysisStage stage, int progress) {
        this.stage = stage;
        this.progress = progress;
    }

    public void complete() {
        this.status = JobStatus.COMPLETED;
        this.stage = null;
        this.progress = 100;
        this.finishedAt = Instant.now();
    }

    public void fail(String errorCode, String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.finishedAt = Instant.now();
    }

    public void reject(String errorCode, String errorMessage) {
        this.status = JobStatus.REJECTED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.finishedAt = Instant.now();
    }

    public void cancel() {
        this.status = JobStatus.CANCELLED;
        this.finishedAt = Instant.now();
    }

    public boolean isInProgress() {
        return JobStatus.IN_PROGRESS.contains(status);
    }
}

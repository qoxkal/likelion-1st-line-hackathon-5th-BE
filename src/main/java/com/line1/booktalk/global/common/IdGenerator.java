package com.line1.booktalk.global.common;

import java.util.UUID;

/**
 * 명세서 예시처럼 앞에 종류를 붙인 id를 만든다.
 * 예: usr_3f2a..., chd_9b1c..., bok_..., bpg_..., job_...
 */
public final class IdGenerator {

    public static final String USER = "usr";
    public static final String CHILD = "chd";
    public static final String BOOK = "bok";
    public static final String BOOK_PAGE = "bpg";
    public static final String ANALYSIS_JOB = "job";

    private IdGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

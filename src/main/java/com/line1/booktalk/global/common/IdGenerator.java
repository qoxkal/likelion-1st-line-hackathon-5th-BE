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
    public static final String EXPRESSION = "exp";
    public static final String REFRESH_SESSION = "rfs";
    public static final String BOOKMARK = "bmk";
    public static final String LEARNING_EVENT = "evt";
    public static final String SENTENCE_PRACTICE = "spr";
    public static final String CONVERSATION_SESSION = "cvs";
    public static final String CONVERSATION_MESSAGE = "msg";
    public static final String REVIEW = "rev";
    public static final String DEVICE = "dev";
    public static final String UPLOAD = "upl";
    public static final String EXPRESSION_TRANSLATION = "etr";

    private IdGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

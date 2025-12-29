package com.grow.study.domain.llm;

public final class AiConstants {

    private AiConstants() {}

    public static final Long AI_WRITER_ID = 0L;
    public static final String AI_WRITER_NICKNAME = "AI 학습 도우미";

    // Rate Limit
    public static final int RATE_LIMIT_PER_MINUTE = 10;
    public static final int BATCH_SIZE = 5;
    public static final int MAX_RETRY_COUNT = 3;
}
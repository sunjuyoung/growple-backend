package com.grow.study.adapter.scheduler;

import com.google.common.util.concurrent.RateLimiter;
import com.grow.study.adapter.persistence.AiAnswerQueueRepository;
import com.grow.study.application.AiAnswerProcessor;
import com.grow.study.domain.llm.AiAnswerQueue;
import com.grow.study.domain.llm.AiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnswerScheduler {

    private final AiAnswerQueueRepository queueRepository;
    private final AiAnswerProcessor processor;

    // 분당 10개 제한
    private final RateLimiter rateLimiter = RateLimiter.create(
            AiConstants.RATE_LIMIT_PER_MINUTE / 60.0
    );

    @Scheduled(fixedDelayString = "${ai.answer.schedule-delay:60000}")
    public void processAiAnswerQueue() {
        List<AiAnswerQueue> pendingItems = queueRepository.findPendingWithLock(AiConstants.BATCH_SIZE);

        if (pendingItems.isEmpty()) {
            return;
        }

        log.info("Processing {} AI answer requests", pendingItems.size());

        for (AiAnswerQueue item : pendingItems) {
            if (!rateLimiter.tryAcquire()) {
                log.debug("Rate limit reached, will process remaining in next batch");
                break;
            }
            processor.process(item);
        }
    }
}
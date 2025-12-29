package com.grow.study.adapter.intergration;

import com.grow.study.adapter.persistence.AiAnswerQueueRepository;
import com.grow.study.application.dto.QuestionPostedEvent;
import com.grow.study.domain.llm.AiAnswerQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnswerEventListener {

    private final AiAnswerQueueRepository queueRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionPosted(QuestionPostedEvent event) {
        if (queueRepository.existsByPostId(event.postId())) {
            log.warn("AI answer queue already exists for post: {}", event.postId());
            return;
        }

        AiAnswerQueue queue = AiAnswerQueue.create(event.post());
        queueRepository.save(queue);

        log.info("AI answer queued for post: {}", event.postId());
    }
}
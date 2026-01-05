package com.grow.study.application;

import com.grow.study.adapter.persistence.AiAnswerQueueRepository;
import com.grow.study.application.dto.QuestionPostedEvent;
import com.grow.study.domain.llm.AiAnswerQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnswerQueueService {

    private final AiAnswerQueueRepository queueRepository;

    @Transactional
    public List<AiAnswerQueue> claimPendingItems(int batchSize) {
        return queueRepository.findPendingWithLock(batchSize);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aiQueueSave(QuestionPostedEvent event) {
        if (queueRepository.existsByPostId(event.postId())) {
            log.warn("AI answer queue already exists for post: {}", event.postId());
            return;
        }

        AiAnswerQueue queue = AiAnswerQueue.create(event.post());
        queueRepository.save(queue);

        log.info("AI answer queued for post: {}", event.postId());
    }


}
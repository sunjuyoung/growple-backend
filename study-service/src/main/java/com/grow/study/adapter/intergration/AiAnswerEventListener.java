package com.grow.study.adapter.intergration;

import com.grow.study.adapter.persistence.AiAnswerQueueRepository;
import com.grow.study.application.AiAnswerQueueService;
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

    private final AiAnswerQueueService queueService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionPosted(QuestionPostedEvent event) {
        queueService.aiQueueSave(event);
    }


}
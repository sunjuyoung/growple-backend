package com.grow.study.adapter.intergration;

import com.grow.study.application.StudyVectorService;
import com.grow.study.domain.event.StudyCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 스터디 벡터 이벤트 리스너
 * 스터디 생성 시 pgvector Document 자동 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyVectorEventListener {

    private final StudyVectorService studyVectorService;
    private final SlackNotifier slackNotifier;

    /**
     * 스터디 생성 후 Document 생성
     * 트랜잭션 커밋 후 비동기로 처리
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyCreatedEvent(StudyCreatedEvent event) {
        try {
            studyVectorService.createStudyDocument(event.studyId(), event);
            log.info("Successfully created vector document for study: {}", event.studyId());
        } catch (Exception e) {
            log.error("Failed to create vector document for study: {}", event.studyId(), e);
            // 벡터 생성 실패는 스터디 생성에 영향을 주지 않음
            // 추후 재시도 로직 추가 가능
            slackNotifier.sendWarning("vector 생성실패",
                    String.format("스터디 ID: %d, 오류: %s", event.studyId(), e.getMessage())
                    );

        }
    }
}

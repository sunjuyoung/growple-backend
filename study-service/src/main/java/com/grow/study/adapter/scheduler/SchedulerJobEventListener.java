package com.grow.study.adapter.scheduler;

import com.grow.study.application.SchedulerJobService;
import com.grow.study.domain.event.StudyStatusChangedEvent;
import com.grow.study.domain.scheduler.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerJobEventListener {

    private final SchedulerJobService schedulerJobService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudyStatusChanged(StudyStatusChangedEvent event) {
        log.info("스터디 상태 변경 이벤트 수신 - studyId: {}, newStatus: {}",
                event.studyId(), event.newStatus());

        switch (event.newStatus()) {
            case RECRUITING -> schedulerJobService.createJobIfNotExists(
                    JobType.RECRUITMENT_DEADLINE,
                    "STUDY",
                    event.studyId(),
                    event.recruitEndDate()
            );
            case RECRUIT_CLOSED -> schedulerJobService.createJobIfNotExists(
                    JobType.STUDY_START,
                    "STUDY",
                    event.studyId(),
                    event.startDate()
            );
            case IN_PROGRESS -> schedulerJobService.createJobIfNotExists(
                    JobType.STUDY_COMPLETION,
                    "STUDY",
                    event.studyId(),
                    event.endDate()
            );
        }
    }
}

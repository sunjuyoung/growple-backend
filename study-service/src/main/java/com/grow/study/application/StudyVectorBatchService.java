package com.grow.study.application;

import com.grow.study.application.required.StudyRepository;
import com.grow.study.domain.event.StudyCreatedEvent;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 스터디 벡터 배치 서비스
 * 기존 스터디들의 Document 일괄 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyVectorBatchService {

    private final StudyRepository studyRepository;
    private final StudyVectorService studyVectorService;

    /**
     * 모든 모집 중인 스터디의 Document 생성
     * 초기 마이그레이션 또는 수동 실행용
     */
    @Transactional(readOnly = true)
    public void createDocumentsForAllRecruitingStudies() {
        log.info("Starting batch creation of vector documents for recruiting studies");

        List<Study> recruitingStudies = studyRepository.findByStatusAndStartDate(
                StudyStatus.RECRUITING,
                LocalDate.now().plusYears(1) // 1년 이내 시작 예정인 스터디
        );

        log.info("Found {} recruiting studies to process", recruitingStudies.size());

        int successCount = 0;
        int failCount = 0;

        for (Study study : recruitingStudies) {
            try {
                StudyCreatedEvent event = new StudyCreatedEvent(
                        study.getId(),
                        study.getTitle(),
                        study.getIntroduction(),
                        study.getCurriculum(),
                        study.getLeaderMessage(),
                        study.getCategory(),
                        study.getLevel(),
                        study.getStatus(),
                        study.getMinParticipants(),
                        study.getMaxParticipants(),
                        study.getSchedule().getStartDate(),
                        study.getSchedule().getEndDate()
                );

                studyVectorService.createStudyDocument(study.getId(), event);
                successCount++;
                log.debug("Created document for study: {}", study.getId());
            } catch (Exception e) {
                failCount++;
                log.error("Failed to create document for study: {}", study.getId(), e);
            }
        }

        log.info("Batch creation completed. Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * 모든 스터디의 Document 재생성
     * 주의: 기존 Document 모두 삭제 후 재생성
     */
    @Transactional(readOnly = true)
    public void recreateAllDocuments() {
        log.warn("Starting recreation of ALL vector documents");

        // 향후 구현: 모든 Document 삭제 후 재생성
        // 현재는 Spring AI VectorStore가 ID 기반으로 덮어쓰기를 지원하므로
        // createDocumentsForAllRecruitingStudies()를 호출하면 됨

        createDocumentsForAllRecruitingStudies();

        log.info("Recreation completed");
    }
}

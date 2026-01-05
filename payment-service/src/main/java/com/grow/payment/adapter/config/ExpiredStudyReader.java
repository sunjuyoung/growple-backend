package com.grow.payment.adapter.config;

import com.grow.payment.application.dto.CompletedStudyForSettlementResponse;
import com.grow.payment.application.required.SettlementRepository;
import com.grow.payment.application.required.StudyRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Step 1 Reader: Study Service에서 완료된 스터디 조회
 *
 * - Study Service API 호출하여 COMPLETED 상태의 스터디 목록 조회
 * - 이미 Settlement이 생성된 스터디는 필터링
 * - 한 번 조회한 데이터를 메모리에 캐싱하여 순차적으로 반환
 */
@Slf4j
@Component
public class ExpiredStudyReader implements ItemReader<ExpiredStudyDto> {

    private static final int FETCH_LIMIT = 100;

    private final StudyRestClient studyRestClient;
    private final SettlementRepository settlementRepository;

    private Iterator<ExpiredStudyDto> studyIterator;
    private boolean initialized = false;

    public ExpiredStudyReader(StudyRestClient studyRestClient,
                              SettlementRepository settlementRepository) {
        this.studyRestClient = studyRestClient;
        this.settlementRepository = settlementRepository;
    }

    @Override
    public ExpiredStudyDto read() {
        if (!initialized) {
            initialize();
        }

        if (studyIterator != null && studyIterator.hasNext()) {
            return studyIterator.next();
        }

        // 다음 배치 실행을 위해 상태 초기화
        initialized = false;
        studyIterator = null;
        return null;
    }

    private void initialize() {
        log.info("정산 대상 스터디 조회 시작");

        // Study Service에서 완료된 스터디 목록 조회
        List<CompletedStudyForSettlementResponse> completedStudies =
                studyRestClient.getCompletedStudiesForSettlement(FETCH_LIMIT);

        if (completedStudies == null || completedStudies.isEmpty()) {
            log.info("정산 대상 스터디 없음");
            studyIterator = null;
            initialized = true;
            return;
        }

        log.info("Study Service에서 {} 건의 완료된 스터디 조회", completedStudies.size());

        // 이미 Settlement이 생성된 스터디 필터링
        List<Long> studyIds = completedStudies.stream()
                .map(CompletedStudyForSettlementResponse::studyId)
                .toList();

        Set<Long> existingSettlementStudyIds =
                Set.copyOf(settlementRepository.findStudyIdsWithSettlement(studyIds));

        List<ExpiredStudyDto> filteredStudies = completedStudies.stream()
                .filter(study -> !existingSettlementStudyIds.contains(study.studyId()))
                .map(ExpiredStudyDto::from)
                .toList();

        log.info("기존 정산 제외 후 {} 건 처리 예정 (제외: {} 건)",
                filteredStudies.size(),
                completedStudies.size() - filteredStudies.size());

        studyIterator = filteredStudies.iterator();
        initialized = true;
    }
}

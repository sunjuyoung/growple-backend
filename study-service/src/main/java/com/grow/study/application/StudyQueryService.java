package com.grow.study.application;

import com.grow.study.adapter.persistence.StudyJpaRepository;
import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.adapter.persistence.dto.StudySearchCondition;
import com.grow.study.adapter.persistence.dto.StudySearchCondition.StudySortType;
import com.grow.study.application.provided.StudyFinder;
import com.grow.study.application.required.*;
import com.grow.study.application.required.dto.MemberSummaryResponse;
import com.grow.study.application.required.dto.StudyWithMemberCountDto;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import com.grow.study.domain.study.DayOfWeek;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudyQueryService implements StudyFinder {

    private final StudyRepository studyRepository;
    private final MemberRestClient memberRestClient;

    @Transactional(readOnly = true)
    @Override
    public StudyWithMemberCountResponse getStudyEnrollmentDetail(Long studyId) {

        Study study = studyRepository.findWithSchedule(studyId).orElseThrow();

        StudyWithMemberCountDto studyWithMemberCountDto = StudyWithMemberCountDto.of(study);


        Set<DayOfWeek> daysOfWeek  = studyWithMemberCountDto.getStudy().getSchedule().getDaysOfWeek();
        Set<String> dayNames = daysOfWeek.stream()
                .map(DayOfWeek::getShortName)
                .collect(Collectors.toSet());

        //member service 연동 필요
        MemberSummaryResponse memberSummary = memberRestClient.getMemberSummary(study.getLeaderId());

        return StudyWithMemberCountResponse.of(studyWithMemberCountDto, memberSummary,dayNames);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<StudyListResponse> getStudyList(
            String level,
            StudyCategory category,
            Integer minDepositAmount,
            Integer maxDepositAmount,
            String sortType,
            Pageable pageable
    ) {
        StudySearchCondition condition = StudySearchCondition.builder()
                .level(parseLevel(level))
                .category(category)
                .minDepositAmount(minDepositAmount)
                .maxDepositAmount(maxDepositAmount)
                .sortType(parseSortType(sortType))
                .build();

        return studyRepository.searchStudyList(condition, pageable);
    }

    @Override
    public CursorResult<StudyListResponse> getStudyListByCursor(String level,
                                                        StudyCategory category,
                                                        Integer minDepositAmount,
                                                        Integer maxDepositAmount,
                                                        String sortType,
                                                        String cursor) {

        StudySearchCondition condition = StudySearchCondition.builder()
                .level(parseLevel(level))
                .category(category)
                .minDepositAmount(minDepositAmount)
                .maxDepositAmount(maxDepositAmount)
                .sortType(parseSortType(sortType))
                .build();

        return  studyRepository.searchStudyListByCursor(condition, cursor, 3);
    }

    private StudyLevel parseLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        return StudyLevel.fromDisplayName(level);
    }

    private StudySortType parseSortType(String sortType) {
        if (sortType == null || sortType.isBlank()) {
            return StudySortType.LATEST;
        }
        return StudySortType.valueOf(sortType);
    }
}

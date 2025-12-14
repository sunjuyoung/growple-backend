package com.grow.study.adapter.persistence;

import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.adapter.persistence.dto.StudySearchCondition;
import com.grow.study.application.required.StudyRepository;
import com.grow.study.application.required.dto.StudyWithMemberCountDto;
import com.grow.study.domain.study.Study;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StudyRepositoryAdapter implements StudyRepository {

    private final StudyJpaRepository studyJpaRepository;


    @Override
    public Optional<Study> findById(Long studyId) {
        return studyJpaRepository.findById(studyId);
    }

    @Override
    public Optional<Study> findStudiesById(Long studyId) {
        return studyJpaRepository.findStudiesById(studyId);
    }

    @Override
    public Optional<Study> findWithSchedule(Long studyId) {
        return studyJpaRepository.findWithSchedule(studyId);
    }

    @Override
    public Long countActiveMembers(Long studyId) {
        return studyJpaRepository.countActiveMembers(studyId);
    }

    @Override
    public Page<StudyListResponse> searchStudyList(StudySearchCondition condition, Pageable pageable) {
        return studyJpaRepository.searchStudyList(condition, pageable);
    }

    @Override
    public CursorResult<StudyListResponse> searchStudyListByCursor(StudySearchCondition condition, String cursor, int size) {
        return studyJpaRepository.searchStudyListByCursor(condition, cursor, size);
    }

    @Override
    public Study save(Study study) {
        return studyJpaRepository.save(study);
    }

    @Override
    public Optional<Study> findStudyDashBoard(Long studyId) {
        return studyJpaRepository.findStudyDashBoard(studyId);
    }


}

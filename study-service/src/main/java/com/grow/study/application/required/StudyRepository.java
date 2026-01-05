package com.grow.study.application.required;

import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.adapter.persistence.dto.StudySearchCondition;
import com.grow.study.application.required.dto.StudyWithMemberCountDto;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyRepository {


    Optional<Study> findById(Long studyId);

    Optional<Study> findStudiesById(Long studyId);

    Optional<Study> findWithSchedule(@Param("studyId") Long studyId);

    Long countActiveMembers(@Param("studyId") Long studyId);

    Page<StudyListResponse> searchStudyList(StudySearchCondition condition, Pageable pageable);

    CursorResult<StudyListResponse> searchStudyListByCursor(StudySearchCondition condition, String cursor, int size);

    Study save(Study study);


    Optional<Study> findStudyDashBoard(@Param("studyId") Long studyId);

    List<Study> findByStatusAndRecruitEndDate(StudyStatus status, LocalDate recruitEndDate);

    List<Study> findByStatusAndStartDate(StudyStatus status, LocalDate startDate);

    List<Study> findByStatusAndEndDateBefore(StudyStatus status, LocalDate today);

    List<Study> findByStatus(StudyStatus status);
}

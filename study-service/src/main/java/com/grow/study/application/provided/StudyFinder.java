package com.grow.study.application.provided;

import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.application.dto.MyStudiesResponse;
import com.grow.study.application.dto.StudyDashboardResponse;
import com.grow.study.application.required.dto.StudySummaryResponse;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import com.grow.study.domain.study.StudyCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudyFinder {

     StudyWithMemberCountResponse getStudyEnrollmentDetail(Long studyId);

     Page<StudyListResponse> getStudyList(
             String level,
             StudyCategory category,
             Integer minDepositAmount,
             Integer maxDepositAmount,
             String sortType,
             Pageable pageable
     );
    CursorResult<StudyListResponse> getStudyListByCursor(
            String level,
            StudyCategory category,
            Integer minDepositAmount,
            Integer maxDepositAmount,
            String sortType,
            String cursor
    );

    StudySummaryResponse getStudySimpleDetail(Long id);


     StudyDashboardResponse getStudyDashboard(Long studyId, Long memberId);

     /**
      * 내 스터디 목록 조회 (참여중, 예정, 완료)
      */
     MyStudiesResponse getMyStudies(Long memberId);
}

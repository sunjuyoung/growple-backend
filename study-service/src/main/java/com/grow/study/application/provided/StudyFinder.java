package com.grow.study.application.provided;

import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import com.grow.study.domain.study.StudyCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudyFinder {

     StudyWithMemberCountResponse getStudyEnrollmentDetail(Long studyId, Long userId);

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
}

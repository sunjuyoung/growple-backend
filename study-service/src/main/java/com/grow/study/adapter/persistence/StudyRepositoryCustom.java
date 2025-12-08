package com.grow.study.adapter.persistence;

import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.adapter.persistence.dto.StudySearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudyRepositoryCustom {

    Page<StudyListResponse> searchStudyList(StudySearchCondition condition, Pageable pageable);

    CursorResult<StudyListResponse> searchStudyListByCursor(StudySearchCondition condition, String cursor, int size);
}

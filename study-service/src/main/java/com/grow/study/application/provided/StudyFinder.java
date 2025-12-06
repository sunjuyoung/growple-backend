package com.grow.study.application.provided;

import com.grow.study.application.required.dto.StudyWithMemberCountResponse;

public interface StudyFinder {

     StudyWithMemberCountResponse getStudyEnrollmentDetail(Long studyId, Long userId);
}
